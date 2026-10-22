import fs from "node:fs"
import path from "node:path"
import ora from "ora"

/**
 * On Android, strings.xml is the single wiring point for Auth0 configuration:
 *   - `com_auth0_domain` / `com_auth0_client_id` are read by the Auth0.Android
 *     SDK at launch, and
 *   - `com_auth0_scheme` is consumed by the `auth0Scheme` manifest placeholder
 *     that drives RedirectActivity (so the OAuth callback reaches the app).
 *
 * Because a single file gates everything, the writer is defensive: it is
 * idempotent, verifies the write by re-reading, never clobbers unrelated string
 * resources, and falls back to printed manual instructions if the file shape is
 * unexpected rather than corrupting it.
 */

const AUTH0_KEYS = ["com_auth0_client_id", "com_auth0_domain", "com_auth0_scheme"]

/**
 * Escape a value for inclusion in an Android XML string resource body.
 */
function escapeXml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
}

/**
 * Print manual fallback instructions for wiring the config by hand.
 */
function printManualInstructions(domain, clientId, scheme, stringsXmlPath) {
  const rel = path.relative(process.cwd(), stringsXmlPath)
  console.log(`\n   Add/update the following in ${rel} manually:\n`)
  console.log(`     <string name="com_auth0_client_id">${clientId}</string>`)
  console.log(`     <string name="com_auth0_domain">${domain}</string>`)
  console.log(`     <string name="com_auth0_scheme">${scheme}</string>\n`)
}

/**
 * Parse the <string name="...">value</string> entries out of a strings.xml body.
 * @returns {Map<string,string>}
 */
function parseStrings(content) {
  const strings = new Map()
  const stringRegex = /<string\s+name="([^"]+)">([\s\S]*?)<\/string>/g
  let match
  while ((match = stringRegex.exec(content)) !== null) {
    strings.set(match[1], match[2])
  }
  return strings
}

/**
 * Write Auth0 configuration to Android strings.xml.
 *
 * Reads the existing strings.xml, updates only the Auth0-related entries,
 * preserves any other custom string resources, then re-reads the file to verify
 * the three Auth0 values actually landed before reporting success.
 *
 * Safe to run repeatedly: if the values are already correct it still rewrites
 * them to the same content (a no-op in practice) and the second run of the
 * bootstrap shows all resources as SKIP.
 *
 * @param {string} domain - Auth0 tenant domain
 * @param {string} clientId - Native client id created/updated by the bootstrap
 * @param {string} scheme - Callback scheme (the sample app uses "demo")
 * @param {string} stringsXmlPath - Absolute path to app strings.xml
 */
export async function writeStringsFile(domain, clientId, scheme, stringsXmlPath) {
  const spinner = ora({
    text: "Generating strings.xml",
  }).start()

  // The file must exist — validateAndroidProject checks this, but guard anyway
  // and fall back to manual instructions rather than creating one blindly.
  if (!fs.existsSync(stringsXmlPath)) {
    spinner.warn(`Could not find strings.xml at ${stringsXmlPath}`)
    printManualInstructions(domain, clientId, scheme, stringsXmlPath)
    return
  }

  try {
    const existingContent = fs.readFileSync(stringsXmlPath, "utf-8")

    // Parse existing strings so we preserve every non-Auth0 resource verbatim.
    const existingStrings = parseStrings(existingContent)

    const desired = {
      com_auth0_client_id: escapeXml(clientId),
      com_auth0_domain: escapeXml(domain),
      com_auth0_scheme: escapeXml(scheme),
    }

    // Update only the Auth0 values; leave everything else untouched.
    for (const [key, value] of Object.entries(desired)) {
      existingStrings.set(key, value)
    }

    // Ensure app_name exists (the manifest references it for the label).
    if (!existingStrings.has("app_name")) {
      existingStrings.set("app_name", "universal_components_android")
    }

    // Build XML in a stable order: app_name, then the Auth0 config, then any
    // remaining custom strings in their original discovery order.
    const orderedKeys = ["app_name", ...AUTH0_KEYS]
    const writtenKeys = new Set()
    const lines = ["<resources>"]

    for (const key of orderedKeys) {
      if (existingStrings.has(key)) {
        lines.push(`    <string name="${key}">${existingStrings.get(key)}</string>`)
        writtenKeys.add(key)
      }
    }
    for (const [key, value] of existingStrings) {
      if (!writtenKeys.has(key)) {
        lines.push(`    <string name="${key}">${value}</string>`)
      }
    }
    lines.push("</resources>")
    lines.push("") // trailing newline

    fs.writeFileSync(stringsXmlPath, lines.join("\n"), "utf-8")

    // Verify: re-read the file and confirm the three Auth0 values are present
    // with the expected content before reporting success — never trust a silent
    // success.
    const verifyStrings = parseStrings(fs.readFileSync(stringsXmlPath, "utf-8"))
    const notApplied = Object.entries(desired).filter(
      ([key, value]) => verifyStrings.get(key) !== value
    )

    if (notApplied.length > 0) {
      spinner.warn("strings.xml did not verify after writing")
      printManualInstructions(domain, clientId, scheme, stringsXmlPath)
      return
    }

    spinner.succeed(`Updated ${path.relative(process.cwd(), stringsXmlPath)}`)
  } catch (e) {
    spinner.fail("Failed to generate strings.xml")
    console.warn(`   ${e.message}`)
    printManualInstructions(domain, clientId, scheme, stringsXmlPath)
  }
}
