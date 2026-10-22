import ora from "ora"

import { auth0ApiCall } from "./auth0-api.mjs"
import { ChangeAction, createChangeItem } from "./change-plan.mjs"
import {
  extractMissingScope,
  isPermissionError,
  recordManualAction,
} from "./manual-actions.mjs"

// Constants
export const DEFAULT_CONNECTION_NAME = "Username-Password-Authentication"

// Passkeys are a first-class authentication method for the sample app's MFA /
// Passkeys UI components. Enabling them on the database connection is what makes
// the "Passkey" option appear in Universal Login (alongside identifier-first).
// `passkey_options` mirror Auth0's recommended progressive-enrollment defaults.
const PASSKEY_CONNECTION_OPTIONS = {
  authentication_methods: {
    password: { enabled: true },
    passkey: { enabled: true },
  },
  passkey_options: {
    challenge_ui: "both",
    local_enrollment_enabled: true,
    progressive_enrollment_enabled: true,
  },
}

/**
 * Read every client id enabled on a connection, draining the `next` cursor.
 *
 * @param {string} connectionId
 * @returns {Promise<string[]|null>} Client ids, or null if the list could not be
 *   read (e.g. a missing scope) — null means "unknown", not "none enabled".
 */
async function fetchConnectionEnabledClients(connectionId) {
  const clientIds = []
  let cursor = null

  do {
    // URLSearchParams rather than manual interpolation: the cursor is an opaque
    // token containing characters that need escaping in a query value.
    const query = cursor ? `?${new URLSearchParams({ from: cursor })}` : ""

    const result = await auth0ApiCall(
      "get",
      `connections/${connectionId}/clients${query}`
    )

    // auth0ApiCall returns null instead of throwing when a scope is missing.
    if (!result) return null

    for (const client of result.clients || []) clientIds.push(client.client_id)
    // `next` is omitted on the final page, which is what ends the loop.
    cursor = result.next || null
  } while (cursor)

  return clientIds
}

/**
 * Enable a client on a connection via the clients sub-resource. Additive — every
 * other enabled client keeps its current state.
 *
 * Succeeds with 204 No Content, so there is no response body to inspect; the
 * caller verifies by re-reading the sub-resource.
 *
 * @param {string} connectionId
 * @param {string} clientId
 */
async function enableClientOnConnection(connectionId, clientId) {
  await auth0ApiCall("patch", `connections/${connectionId}/clients`, [
    { client_id: clientId, status: true },
  ])
}

// ============================================================================
// CHECK FUNCTIONS
// ============================================================================

export async function checkDatabaseConnectionChanges(
  existingConnections,
  dashboardClientId
) {
  const existing = existingConnections.find(
    (c) => c.name === DEFAULT_CONNECTION_NAME
  )

  if (!existing) {
    return createChangeItem(ChangeAction.CREATE, {
      resource: "Database Connection",
      name: DEFAULT_CONNECTION_NAME,
    })
  }

  const enabledClients = await fetchConnectionEnabledClients(existing.id)

  const clientNeedsEnabling =
    enabledClients === null || !enabledClients.includes(dashboardClientId)

  // Check whether passkeys are enabled on the connection. If not, the sample
  // app's Passkeys UI component has nothing to surface in Universal Login.
  const passkeyEnabled =
    existing.options?.authentication_methods?.passkey?.enabled === true

  const changes = []
  if (clientNeedsEnabling) {
    changes.push("Enable the native app on the connection")
  }
  if (!passkeyEnabled) {
    changes.push("Enable passkey authentication method")
  }

  if (changes.length > 0) {
    return createChangeItem(ChangeAction.UPDATE, {
      resource: "Database Connection",
      name: DEFAULT_CONNECTION_NAME,
      existing,
      // Only enablePasskey is carried forward. Whether the app needs enabling is
      // deliberately NOT recorded here — the apply phase re-resolves it against
      // the live sub-resource, because this check may have run against the
      // TO_BE_CREATED placeholder rather than a real client id.
      updates: { enablePasskey: !passkeyEnabled },
      summary: changes.join(", "),
    })
  }

  return createChangeItem(ChangeAction.SKIP, {
    resource: "Database Connection",
    name: DEFAULT_CONNECTION_NAME,
    existing,
  })
}

// ============================================================================
// APPLY FUNCTIONS
// ============================================================================

export async function applyDatabaseConnectionChanges(
  changePlan,
  dashboardClientId
) {
  if (changePlan.action === ChangeAction.SKIP) {
    const spinner = ora({
      text: `Database Connection is up to date: ${changePlan.name}`,
    }).start()
    spinner.succeed()
    return changePlan.existing
  }

  if (changePlan.action === ChangeAction.CREATE) {
    const spinner = ora({
      text: `Creating Database Connection: ${DEFAULT_CONNECTION_NAME}`,
    }).start()

    try {
      const connectionData = {
        strategy: "auth0",
        name: DEFAULT_CONNECTION_NAME,
        display_name: "Universal-Components",
        options: PASSKEY_CONNECTION_OPTIONS,
      }

      const connection = await auth0ApiCall(
        "post",
        "connections",
        connectionData
      )
      if (!connection?.id) {
        throw new Error(
          "Create returned no connection id (the identity may lack create:connections)"
        )
      }

      await enableClientOnConnection(connection.id, dashboardClientId)

      const enabled = await fetchConnectionEnabledClients(connection.id)
      if (enabled !== null && !enabled.includes(dashboardClientId)) {
        spinner.warn(
          `Created ${DEFAULT_CONNECTION_NAME} but could not enable the native app on it`
        )
        recordManualActionForClient()
        return connection
      }

      spinner.succeed(`Created Database Connection: ${DEFAULT_CONNECTION_NAME}`)
      return connection
    } catch (e) {
      spinner.fail(`Failed to create Database Connection`)
      throw e
    }
  }

  if (changePlan.action === ChangeAction.UPDATE) {
    const spinner = ora({
      text: `Updating ${DEFAULT_CONNECTION_NAME} connection`,
    }).start()

    try {
      const { existing, updates } = changePlan

      // Re-resolve enablement against the live sub-resource using the REAL
      // client id. The change plan may have been built against the
      // TO_BE_CREATED placeholder, so its verdict cannot be trusted here.
      const enabledClients = await fetchConnectionEnabledClients(existing.id)
      const needsClient =
        enabledClients === null || !enabledClients.includes(dashboardClientId)

      // Only the `options` half still goes to the connection object itself.
      // Merge with the existing options so unrelated settings (password policy,
      // attributes, MFA) are preserved.
      const existingOptions = existing.options || {}
      const optionsPatch = updates?.enablePasskey
        ? {
            ...existingOptions,
            authentication_methods: {
              ...(existingOptions.authentication_methods || {}),
              password: { enabled: true },
              passkey: { enabled: true },
            },
            passkey_options: {
              ...PASSKEY_CONNECTION_OPTIONS.passkey_options,
              ...(existingOptions.passkey_options || {}),
            },
          }
        : null

      if (!needsClient && !optionsPatch) {
        spinner.succeed(
          `${DEFAULT_CONNECTION_NAME} connection is already up to date`
        )
        return existing
      }

      if (needsClient) {
        await enableClientOnConnection(existing.id, dashboardClientId)
      }

      if (optionsPatch) {
        await auth0ApiCall("patch", `connections/${existing.id}`, {
          options: optionsPatch,
        })
      }

      // auth0ApiCall swallows missing-scope errors (returns null instead of
      // throwing), and the clients PATCH returns an empty 204 either way, so a
      // "success" above is not proof the change landed. Re-read both resources
      // and verify the intended state actually applied; if not, treat it as a
      // manual action rather than reporting a false success.
      const updated =
        (await auth0ApiCall("get", `connections/${existing.id}`)) || existing
      const enabledAfter = needsClient
        ? await fetchConnectionEnabledClients(existing.id)
        : enabledClients

      const clientApplied =
        !needsClient ||
        enabledAfter === null ||
        enabledAfter.includes(dashboardClientId)
      const passkeyApplied =
        !optionsPatch ||
        updated.options?.authentication_methods?.passkey?.enabled === true

      if (clientApplied && passkeyApplied) {
        const applied = []
        if (needsClient) applied.push("native app enabled")
        if (optionsPatch) applied.push("passkey method")
        spinner.succeed(
          `Updated ${DEFAULT_CONNECTION_NAME} connection (${applied.join(", ")})`
        )
        return updated
      }

      spinner.warn(`Could not fully update ${DEFAULT_CONNECTION_NAME} connection`)

      if (!clientApplied) recordManualActionForClient()
      if (!passkeyApplied) {
        // Writing the connection `options` object (which is where the passkey
        // authentication method lives) requires update:connections_options — a
        // scope distinct from update:connections. When it is missing the CLI
        // reports an empty "lacks scope: ." because it cannot render the newer
        // scope name, so we name it explicitly here.
        recordManualAction({
          resource: `Connection: ${DEFAULT_CONNECTION_NAME} (enable passkeys)`,
          scope: "update:connections_options",
          reason:
            "Passkey must be enabled on the connection for the Passkey login option to appear in Universal Login. Writing connection options requires update:connections_options (separate from update:connections).",
          manualStep:
            "Grant update:connections_options to the M2M app and re-run, OR Dashboard → Authentication → Database → <connection> → Authentication Methods → toggle Passkey on.",
        })
      }

      return updated
    } catch (e) {
      if (isPermissionError(e)) {
        const scope = extractMissingScope(e) || "update:connections"
        spinner.warn(
          `Skipped updating ${DEFAULT_CONNECTION_NAME} — M2M app lacks scope: ${scope}`
        )
        recordManualAction({
          resource: `Connection: ${DEFAULT_CONNECTION_NAME}`,
          scope,
          reason:
            "The native app must be an enabled client of this connection and passkeys enabled for the full login experience.",
          manualStep:
            "Dashboard → Authentication → Database → enable the app + toggle Passkey, OR grant the scope above and re-run.",
        })
        return changePlan.existing
      }
      spinner.fail(`Failed to update ${DEFAULT_CONNECTION_NAME} connection`)
      throw e
    }
  }
}

// Recorded from both the create and update paths: enabling the app is a separate
// call from creating/patching the connection, so either path can land the
// connection but miss the enablement.
function recordManualActionForClient() {
  recordManualAction({
    resource: `Connection: ${DEFAULT_CONNECTION_NAME} (enable app)`,
    scope: "update:connections",
    reason:
      "The native app must be an enabled client of this database connection for username/password login to work.",
    manualStep:
      "Dashboard → Authentication → Database → Applications → enable the app, OR grant update:connections and re-run.",
  })
}
