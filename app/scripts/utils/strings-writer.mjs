import fs from "node:fs"
import path from "node:path"
import ora from "ora"

/**
 * Write Auth0 configuration to Android strings.xml
 *
 * Reads the existing strings.xml, updates Auth0-related entries,
 * and preserves any other custom string resources.
 */
export async function writeStringsFile(domain, clientId, scheme, stringsXmlPath) {
  const spinner = ora({
    text: "Generating strings.xml",
  }).start()

  try {
    // Read existing file to preserve non-Auth0 strings
    let existingContent = ""
    if (fs.existsSync(stringsXmlPath)) {
      existingContent = fs.readFileSync(stringsXmlPath, "utf-8")
    }

    // Parse existing strings (simple regex-based parser for Android strings.xml)
    const existingStrings = new Map()
    const stringRegex =
      /<string\s+name="([^"]+)">([\s\S]*?)<\/string>/g
    let match
    while ((match = stringRegex.exec(existingContent)) !== null) {
      existingStrings.set(match[1], match[2])
    }

    // Update Auth0 values
    existingStrings.set("com_auth0_client_id", clientId)
    existingStrings.set("com_auth0_domain", domain)
    existingStrings.set("com_auth0_scheme", scheme)

    // Ensure app_name exists
    if (!existingStrings.has("app_name")) {
      existingStrings.set("app_name", "ui_components_android")
    }

    // Build XML output
    const lines = ['<resources>']

    // Write strings in a consistent order: app_name first, then Auth0 config, then others
    const orderedKeys = ["app_name", "com_auth0_client_id", "com_auth0_domain", "com_auth0_scheme"]
    const writtenKeys = new Set()

    for (const key of orderedKeys) {
      if (existingStrings.has(key)) {
        lines.push(`    <string name="${key}">${existingStrings.get(key)}</string>`)
        writtenKeys.add(key)
      }
    }

    // Write remaining strings
    for (const [key, value] of existingStrings) {
      if (!writtenKeys.has(key)) {
        lines.push(`    <string name="${key}">${value}</string>`)
      }
    }

    lines.push('</resources>')
    lines.push('') // trailing newline

    const xmlContent = lines.join("\n")

    // Write file
    fs.writeFileSync(stringsXmlPath, xmlContent, "utf-8")

    spinner.succeed(`Updated ${path.relative(process.cwd(), stringsXmlPath)}`)
  } catch (e) {
    spinner.fail("Failed to generate strings.xml")
    throw e
  }
}
