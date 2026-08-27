#!/usr/bin/env node
import {
  applyDashboardClientChanges,
  applyMyAccountClientGrantChanges,
  DEFAULT_CLIENT_NAME,
  setClientName,
} from "./utils/clients.mjs"
import { applyDatabaseConnectionChanges } from "./utils/connections.mjs"
import {
  buildChangePlan,
  discoverExistingResources,
  displayChangePlan,
} from "./utils/discovery.mjs"
import { writeStringsFile } from "./utils/strings-writer.mjs"
import { confirmWithUser, promptWithUser } from "./utils/helpers.mjs"
import { getManualActions } from "./utils/manual-actions.mjs"
import {
  applyMyAccountResourceServerChanges,
  MY_ACCOUNT_API_SCOPES,
} from "./utils/resource-servers.mjs"
import { applyAdminRoleChanges } from "./utils/roles.mjs"
import {
  applyPromptSettingsChanges,
  applyTenantSettingsChanges,
} from "./utils/tenant-config.mjs"
import { applyGuardianFactorChanges } from "./utils/guardian-factors.mjs"
import {
  checkAuth0CLI,
  checkNodeVersion,
  hasMachineCredentials,
  printScopeUsageDetails,
  validateAndroidProject,
  validateAuth0Session,
  validateMyAccountScopes,
  validateTenant,
} from "./utils/validation.mjs"
import { ChangeAction } from "./utils/change-plan.mjs"

// ============================================================================
// Main Bootstrap Flow
// ============================================================================

async function main() {
  console.log("\n🚀 Auth0 Android Universal Components - Bootstrap Script\n")

  // Parse command-line arguments
  const args = process.argv.slice(2)

  if (args.includes("--help") || args.includes("-h")) {
    console.log(
      "Usage: npm run auth0:bootstrap <tenant-domain> [--yes] [--client-name=<name>]"
    )
    console.log("\nArguments:")
    console.log(
      "  tenant-domain  Required. The Auth0 tenant domain to configure."
    )
    console.log("                 Must match your Auth0 CLI active tenant.")
    console.log("\nOptions:")
    console.log(
      "  --yes, -y      Skip the confirmation prompt and apply changes."
    )
    console.log(
      "                 Auto-enabled when M2M credentials are set and stdin is"
    )
    console.log("                 not a TTY (headless/CI). Env: AUTH0_BOOTSTRAP_YES=1")
    console.log("  --client-name=<name>")
    console.log(
      "                 Name of the native Auth0 application. Interactive runs"
    )
    console.log(
      `                 are prompted for it; the default is "${DEFAULT_CLIENT_NAME}".`
    )
    console.log("                 Env: AUTH0_CLIENT_NAME")
    console.log(
      "                 The name is the lookup key on re-runs: reusing a name"
    )
    console.log(
      "                 updates that application, a new name creates a new one."
    )
    console.log("\nExample:")
    console.log("  npm run auth0:bootstrap my-tenant.us.auth0.com")
    console.log("\nPrerequisites:")
    console.log("  1. Install Auth0 CLI: https://github.com/auth0/auth0-cli")
    console.log(
      "\nNote: The script checks your Auth0 CLI session and, if needed, logs you"
    )
    console.log(
      "  in and switches to the requested tenant automatically."
    )
    console.log("\nNon-interactive (standalone / CI) login:")
    console.log(
      "  Set these environment variables for a Management-API M2M app and the"
    )
    console.log(
      "  script authenticates via client credentials — no browser required:"
    )
    console.log("    AUTH0_CLIENT_ID      Client ID of the M2M application")
    console.log("    AUTH0_CLIENT_SECRET  Client secret of the M2M application")
    console.log(
      "    AUTH0_DOMAIN         Tenant domain (optional; defaults to the arg)"
    )
    console.log(
      "\nNote: Tenant name is required as a safety measure to prevent accidentally"
    )
    console.log("  configuring the wrong tenant.")
    // Expand the full per-scope rationale for users who want to know why each
    // Management API permission is requested at login.
    printScopeUsageDetails()
    process.exit(0)
  }

  // Flags: --yes/-y (or AUTH0_BOOTSTRAP_YES) skip the confirm prompt. The first
  // non-flag argument is the tenant domain.
  const flags = args.filter((a) => a.startsWith("-"))
  const tenantName = args.find((a) => !a.startsWith("-"))
  const yesFlag =
    flags.includes("--yes") ||
    flags.includes("-y") ||
    process.env.AUTH0_BOOTSTRAP_YES === "1"

  // Whether this run may prompt at all: --yes, or a non-interactive M2M run (no
  // TTY), means every prompt — the client name and the apply confirmation — must
  // fall back to its default instead of blocking.
  const autoConfirm =
    yesFlag || (hasMachineCredentials(tenantName) && !process.stdin.isTTY)

  // Consistent step numbering across the run. TOTAL is the count of top-level
  // steps below; bump it if you add/remove one.
  const TOTAL_STEPS = 5
  let stepNo = 0
  const step = (emoji, title) =>
    console.log(`\n${emoji} Step ${++stepNo}/${TOTAL_STEPS}: ${title}`)

  // Step 1: Validation
  step("📋", "Pre-flight Checks")
  checkNodeVersion()
  await checkAuth0CLI()
  await validateAuth0Session(tenantName)
  const domain = await validateTenant(tenantName)
  const androidConfig = validateAndroidProject()
  // Auth0.Android's WebAuthProvider uses the configured scheme (mirrored by the
  // auth0Scheme manifest placeholder) for the custom-scheme callback. The sample
  // app's MainActivity calls .withScheme("demo"), so the registered redirect and
  // the strings.xml value must both be "demo" for the OAuth redirect to reach it.
  const scheme = "demo"
  androidConfig.scheme = scheme
  // Resolve the native application's name before discovery: it is the key used
  // to find an existing app, so the change plan depends on it.
  setClientName(await resolveClientName(flags, autoConfirm))

  // Step 2: Discovery
  step("🔍", "Resource Discovery")
  const resources = await discoverExistingResources()
  validateMyAccountScopes(resources, domain)

  // Step 3: Build Change Plan
  step("📝", "Analyzing Changes")
  const plan = await buildChangePlan(resources, domain, androidConfig)
  console.log("")

  // Step 4: Display Plan
  displayChangePlan(plan)

  // Flatten the plan into a single list so change detection and the end-of-run
  // summary work off the same source of truth.
  const planItems = [
    plan.tenantConfig.settings,
    plan.tenantConfig.prompts,
    plan.resourceServer,
    plan.clients.dashboard,
    plan.clientGrants.myAccount,
    plan.connection,
    plan.roles.admin,
    plan.guardianFactors,
  ]
  const countByAction = (action) =>
    planItems.filter((i) => i.action === action).length
  const hasChanges = planItems.some((i) => i.action !== ChangeAction.SKIP)

  if (!hasChanges) {
    console.log(
      "✅ Bootstrap complete! Tenant is already properly configured.\n"
    )
    const confirmed = await confirmWithUser(
      "Do you want to regenerate the strings.xml file?"
    )
    if (confirmed) {
      await writeStringsFile(
        domain,
        plan.clients.dashboard.existing?.client_id,
        scheme,
        androidConfig.stringsXmlPath
      )
      console.log("\n✅ strings.xml updated!\n")
    }

    // Even when nothing changed on the tenant, still show how to build and run
    // the sample app — this is the path taken on every re-run of a
    // fully-configured tenant, so the build/run guidance must not be skipped.
    printNextSteps()
    process.exit(0)
  }

  // User Confirmation. Skipped when --yes is set, or automatically in a
  // non-interactive M2M run (no TTY) so the "standalone/CI" path doesn't hang.
  if (autoConfirm) {
    console.log(
      `\n▶️  Proceeding with ${countByAction(ChangeAction.CREATE)} create, ` +
        `${countByAction(ChangeAction.UPDATE)} update ` +
        `(auto-confirmed${yesFlag ? " via --yes" : " — non-interactive M2M run"}).`
    )
  } else {
    const confirmed = await confirmWithUser(
      "Do you want to proceed with these changes? "
    )
    if (!confirmed) {
      console.log("\n❌ Bootstrap cancelled by user.\n")
      process.exit(0)
    }
  }
  console.log("")

  // Step 4: Apply Changes
  step("⚙️ ", "Applying Changes")
  console.log("")

  // 4a. Tenant Configuration
  console.log("Configuring Tenant...")
  await applyTenantSettingsChanges(plan.tenantConfig.settings)
  await applyPromptSettingsChanges(plan.tenantConfig.prompts)
  console.log("")

  // 4b. Resource Server (My Account API)
  console.log("Configuring My Account API...")
  await applyMyAccountResourceServerChanges(plan.resourceServer, domain)
  console.log("")

  // 4c. Native Client
  console.log("Configuring Native Client...")
  const dashboardClient = await applyDashboardClientChanges(
    plan.clients.dashboard,
    domain,
    MY_ACCOUNT_API_SCOPES
  )
  console.log("")

  // 4d. Client Grants
  console.log("Configuring Client Grants...")
  await applyMyAccountClientGrantChanges(
    plan.clientGrants.myAccount,
    domain,
    dashboardClient.client_id
  )
  console.log("")

  // 4e. Database Connection
  console.log("Configuring Database Connection...")
  await applyDatabaseConnectionChanges(
    plan.connection,
    dashboardClient.client_id
  )
  console.log("")

  // 4f. Roles
  console.log("Configuring Roles...")
  await applyAdminRoleChanges(plan.roles.admin)
  console.log("")

  // 4g. MFA Factors (WebAuthn / Passkey)
  console.log("Configuring MFA Factors...")
  await applyGuardianFactorChanges(plan.guardianFactors)
  console.log("")

  // Step 5: Generate strings.xml (domain + client id + callback scheme). On
  // Android this single file wires both the SDK config and the RedirectActivity
  // (via the auth0Domain / auth0Scheme manifest placeholders).
  step("📝", "Generating strings.xml")
  console.log("")
  await writeStringsFile(
    domain,
    dashboardClient.client_id,
    scheme,
    androidConfig.stringsXmlPath
  )

  // Done!
  console.log("\n✅ Bootstrap complete!\n")

  // Summary of what the plan intended, plus anything that needs manual follow-up.
  const manualCount = getManualActions().length
  console.log(
    `Summary: ${countByAction(ChangeAction.CREATE)} created, ` +
      `${countByAction(ChangeAction.UPDATE)} updated, ` +
      `${countByAction(ChangeAction.SKIP)} already up to date` +
      (manualCount > 0 ? `, ${manualCount} need manual attention` : "") +
      ".\n"
  )

  reportManualActions()

  printNextSteps()
}

/**
 * Reject client names the Management API would reject, before spending a
 * round-trip on them. Auth0 requires a non-empty name without `<` or `>`.
 *
 * @param {string} name - Candidate client name (already trimmed)
 * @returns {string | null} An error message, or null when the name is valid
 */
function validateClientName(name) {
  if (!name) return "Name cannot be empty."
  if (/[<>]/.test(name)) return "Name cannot contain < or >."
  return null
}

/**
 * Decide what to name the native Auth0 application for this run.
 *
 * Precedence: `--client-name=<name>` → `AUTH0_CLIENT_NAME` → interactive prompt
 * (defaulting to DEFAULT_CLIENT_NAME) → the default itself when the run cannot
 * prompt (--yes, or a headless M2M run).
 *
 * The interactive path loops until the user accepts a valid name, so a typo does
 * not silently create a stray application in the tenant.
 *
 * @param {string[]} flags - CLI flags from argv
 * @param {boolean} autoConfirm - True when the run must not prompt
 * @returns {Promise<string>} The client name to use
 */
async function resolveClientName(flags, autoConfirm) {
  const prefix = "--client-name="
  const flagValue = flags
    .find((f) => f.startsWith(prefix))
    ?.slice(prefix.length)
    .trim()
  const preset = flagValue || process.env.AUTH0_CLIENT_NAME?.trim()

  if (preset) {
    const error = validateClientName(preset)
    if (error) {
      throw new Error(
        `Invalid client name "${preset}" (--client-name / AUTH0_CLIENT_NAME): ${error}`
      )
    }
    console.log(`✅ Native application name: ${preset}`)
    return preset
  }

  // Also covers a TTY-less run without M2M credentials: there is nobody to
  // answer the prompt, so take the default rather than looping on empty input.
  if (autoConfirm || !process.stdin.isTTY) {
    console.log(`✅ Native application name: ${DEFAULT_CLIENT_NAME} (default)`)
    return DEFAULT_CLIENT_NAME
  }

  console.log(
    "\n🏷️  Name of the native Auth0 application to create or update.\n" +
      "   Reusing a name updates that application; a new name creates a new one.\n" +
      "   Press Enter to accept the default."
  )

  for (;;) {
    const answer = await promptWithUser(
      "   Application name",
      DEFAULT_CLIENT_NAME
    )

    const error = validateClientName(answer)
    if (error) {
      console.log(`   ⚠️  ${error}`)
      continue
    }

    if (await confirmWithUser(`   Use "${answer}" as the application name?`)) {
      return answer
    }
    console.log("   No problem — enter a different name.")
  }
}

/**
 * Print the build-and-run guidance shown at the end of the bootstrap. Kept as a
 * shared helper so it is printed on BOTH exit paths — after a full apply run and
 * after the "already configured" short-circuit — rather than only when changes
 * were applied.
 */
function printNextSteps() {
  console.log("Next steps:")
  console.log(
    "  1. Review the updated app/src/main/res/values/strings.xml (domain,"
  )
  console.log(
    "     client id, and callback scheme have already been configured for you)."
  )
  console.log(
    "  2. From the project root, build the debug APK of the sample app:"
  )
  console.log("       ./gradlew :app:assembleDebug")
  console.log(
    "  3. With an emulator running or a device connected (adb devices), install"
  )
  console.log("     and launch the sample app:")
  console.log("       ./gradlew :app:installDebug")
  console.log(
    "       adb shell am start -n com.auth0.android.sample/.MainActivity"
  )
  console.log("  4. Log in and explore the Passkey + MFA components.\n")
  console.log(
    "  ⚠️  If the Gradle build fails (e.g. it cannot download dependencies or"
  )
  console.log(
    "     times out resolving artifacts), turn off your VPN if connected and"
  )
  console.log("     try the build again.\n")
}

/**
 * Print a consolidated list of operations that were skipped because the
 * authenticated identity lacked the required Management API scope. Each entry
 * names the scope to grant (or the dashboard step to perform) so the tenant
 * can be finished without re-running everything blindly.
 */
function reportManualActions() {
  const actions = getManualActions()
  if (actions.length === 0) return

  console.log(
    "⚠️  Some steps need manual attention (the login identity lacked the scope):\n"
  )
  actions.forEach((a, i) => {
    console.log(`  ${i + 1}. ${a.resource}`)
    if (a.scope) console.log(`     Missing scope: ${a.scope}`)
    if (a.reason) console.log(`     Why it matters: ${a.reason}`)
    if (a.manualStep) console.log(`     Fix: ${a.manualStep}`)
    console.log("")
  })
  console.log(
    "  Tip: grant the scope(s) above to your M2M app (Dashboard → Applications →"
  )
  console.log(
    "  APIs → Auth0 Management API → Machine to Machine Applications), then re-run"
  )
  console.log(
    "  the bootstrap. It is idempotent — completed resources will be skipped.\n"
  )
}

// Run the main function
main().catch((error) => {
  console.error("\n❌ Bootstrap failed:", error.message)
  process.exit(1)
})
