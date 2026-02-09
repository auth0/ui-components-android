#!/usr/bin/env node
import {
  applyDashboardClientChanges,
  applyMyAccountClientGrantChanges,
} from "./utils/clients.mjs"
import { applyDatabaseConnectionChanges } from "./utils/connections.mjs"
import {
  buildChangePlan,
  discoverExistingResources,
  displayChangePlan,
} from "./utils/discovery.mjs"
import { writeStringsFile } from "./utils/strings-writer.mjs"
import { confirmWithUser } from "./utils/helpers.mjs"
import {
  applyConnectionProfileChanges,
  applyUserAttributeProfileChanges,
} from "./utils/profiles.mjs"
import {
  applyMyAccountResourceServerChanges,
  MY_ACCOUNT_API_SCOPES,
} from "./utils/resource-servers.mjs"
import { applyAdminRoleChanges } from "./utils/roles.mjs"
import {
  applyPromptSettingsChanges,
  applyTenantSettingsChanges,
} from "./utils/tenant-config.mjs"
import {
  checkAuth0CLI,
  checkNodeVersion,
  validateAndroidProject,
  validateTenant,
} from "./utils/validation.mjs"

// ============================================================================
// Main Bootstrap Flow
// ============================================================================

async function main() {
  console.log("\n🚀 Auth0 Android UI Components - Bootstrap Script\n")

  // Parse command-line arguments
  const args = process.argv.slice(2)

  if (args.includes("--help") || args.includes("-h")) {
    console.log("Usage: npm run auth0:bootstrap <tenant-domain>")
    console.log("\nArguments:")
    console.log(
      "  tenant-domain  Required. The Auth0 tenant domain to configure."
    )
    console.log("                 Must match your Auth0 CLI active tenant.")
    console.log("\nExample:")
    console.log("  npm run auth0:bootstrap my-tenant.us.auth0.com")
    console.log("\nPrerequisites:")
    console.log("  1. Install Auth0 CLI: https://github.com/auth0/auth0-cli")
    console.log("  2. Login to Auth0 CLI: auth0 login")
    console.log("  3. Select your tenant: auth0 tenants use <tenant-domain>")
    console.log(
      "\nNote: Tenant name is required as a safety measure to prevent accidentally"
    )
    console.log("  configuring the wrong tenant.")
    process.exit(0)
  }

  const tenantName = args[0]

  // Step 1: Validation
  console.log("📋 Step 1: Pre-flight Checks")
  checkNodeVersion()
  await checkAuth0CLI()
  const domain = await validateTenant(tenantName)
  const androidConfig = validateAndroidProject()
  const scheme = "demo" // Default scheme for Android sample app
  androidConfig.scheme = scheme
  console.log("")

  // Step 2: Discovery
  console.log("🔍 Step 2: Resource Discovery")
  const resources = await discoverExistingResources(domain)
  console.log("")

  // Step 3: Build Change Plan
  console.log("📝 Step 3: Analyzing Changes")
  const plan = await buildChangePlan(resources, domain, androidConfig)
  console.log("")

  // Step 4: Display Plan
  displayChangePlan(plan)

  // Check if there are any changes to apply
  const hasChanges =
    plan.clients.dashboard.action !== "skip" ||
    plan.clientGrants.myAccount.action !== "skip" ||
    plan.connection.action !== "skip" ||
    plan.connectionProfile.action !== "skip" ||
    plan.userAttributeProfile.action !== "skip" ||
    plan.resourceServer.action !== "skip" ||
    plan.roles.admin.action !== "skip" ||
    plan.tenantConfig.settings.action !== "skip" ||
    plan.tenantConfig.prompts.action !== "skip"

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

    process.exit(0)
  }

  // Step 5: User Confirmation
  const confirmed = await confirmWithUser(
    "Do you want to proceed with these changes? "
  )
  if (!confirmed) {
    console.log("\n❌ Bootstrap cancelled by user.\n")
    process.exit(0)
  }
  console.log("")

  // Step 6: Apply Changes
  console.log("⚙️  Step 4: Applying Changes\n")

  // 6a. Tenant Configuration
  console.log("Configuring Tenant...")
  await applyTenantSettingsChanges(plan.tenantConfig.settings)
  await applyPromptSettingsChanges(plan.tenantConfig.prompts)
  console.log("")

  // 6b. Profiles
  console.log("Configuring Profiles...")
  const connectionProfile = await applyConnectionProfileChanges(
    plan.connectionProfile
  )
  const userAttributeProfile = await applyUserAttributeProfileChanges(
    plan.userAttributeProfile
  )
  console.log("")

  // 6c. Resource Server (My Account API)
  console.log("Configuring My Account API...")
  await applyMyAccountResourceServerChanges(plan.resourceServer, domain)
  console.log("")

  // 6d. Native Client
  console.log("Configuring Native Client...")
  const dashboardClient = await applyDashboardClientChanges(
    plan.clients.dashboard,
    connectionProfile?.id,
    userAttributeProfile?.id,
    domain,
    MY_ACCOUNT_API_SCOPES
  )
  console.log("")

  // 6e. Client Grants
  console.log("Configuring Client Grants...")
  await applyMyAccountClientGrantChanges(
    plan.clientGrants.myAccount,
    domain,
    dashboardClient.client_id
  )
  console.log("")

  // 6f. Database Connection
  console.log("Configuring Database Connection...")
  const connection = await applyDatabaseConnectionChanges(
    plan.connection,
    dashboardClient.client_id
  )
  console.log("")

  // 6g. Roles
  console.log("Configuring Roles...")
  await applyAdminRoleChanges(plan.roles.admin)
  console.log("")

  // Step 7: Generate strings.xml
  console.log("📝 Step 5: Generating strings.xml\n")
  await writeStringsFile(
    domain,
    dashboardClient.client_id,
    scheme,
    androidConfig.stringsXmlPath
  )

  // Done!
  console.log("\n✅ Bootstrap complete!\n")
  console.log("Next steps:")
  console.log("  1. Review the updated strings.xml file")
  console.log("  2. Build the sample app:")
  console.log("     ./gradlew :app:assembleDebug")
  console.log("  3. Run the sample app:")
  console.log("     ./gradlew :app:installDebug")
  console.log("  4. Login and explore the MFA components\n")
}

// Run the main function
main().catch((error) => {
  console.error("\n❌ Bootstrap failed:", error.message)
  process.exit(1)
})
