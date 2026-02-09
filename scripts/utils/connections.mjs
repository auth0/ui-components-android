import { $ } from "execa"
import ora from "ora"

import { auth0ApiCall } from "./auth0-api.mjs"
import { ChangeAction, createChangeItem } from "./change-plan.mjs"

// Constants
export const DEFAULT_CONNECTION_NAME = "Universal-Components-Demo"

// ============================================================================
// CHECK FUNCTIONS
// ============================================================================

export function checkDatabaseConnectionChanges(
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
      dashboardClientId,
    })
  }

  // Check if the dashboard client is already enabled
  const enabledClients = existing.enabled_clients || []
  if (dashboardClientId && !enabledClients.includes(dashboardClientId)) {
    return createChangeItem(ChangeAction.UPDATE, {
      resource: "Database Connection",
      name: DEFAULT_CONNECTION_NAME,
      existing,
      dashboardClientId,
      updates: {
        enabled_clients: [...enabledClients, dashboardClientId],
      },
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
        enabled_clients: dashboardClientId ? [dashboardClientId] : [],
      }

      const createArgs = [
        "api",
        "post",
        "connections",
        "--data",
        JSON.stringify(connectionData),
      ]

      const { stdout } = await $`auth0 ${createArgs}`
      const connection = JSON.parse(stdout)

      spinner.succeed(`Created Database Connection: ${DEFAULT_CONNECTION_NAME}`)
      return connection
    } catch (e) {
      spinner.fail(`Failed to create Database Connection`)
      throw e
    }
  }

  if (changePlan.action === ChangeAction.UPDATE) {
    const spinner = ora({
      text: `Updating Database Connection: ${DEFAULT_CONNECTION_NAME}`,
    }).start()

    try {
      const updateArgs = [
        "api",
        "patch",
        `connections/${changePlan.existing.id}`,
        "--data",
        JSON.stringify(changePlan.updates),
      ]

      await $`auth0 ${updateArgs}`

      // Fetch updated connection
      const getArgs = [
        "api",
        "get",
        `connections/${changePlan.existing.id}`,
      ]
      const { stdout } = await $`auth0 ${getArgs}`
      const connection = JSON.parse(stdout)

      spinner.succeed(
        `Updated Database Connection: ${DEFAULT_CONNECTION_NAME}`
      )
      return connection
    } catch (e) {
      spinner.fail(`Failed to update Database Connection`)
      throw e
    }
  }
}
