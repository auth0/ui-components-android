import readline from "node:readline/promises"

/**
 * Wait for user confirmation before proceeding
 */
export async function confirmWithUser(message) {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  })

  const answer = await rl.question(`${message} (y/N): `)
  rl.close()

  return answer.toLowerCase() === "y" || answer.toLowerCase() === "yes"
}

/**
 * Ask the user for a free-text value, falling back to `defaultValue` when the
 * answer is blank. Returns `defaultValue` immediately when stdin is not a TTY
 * (headless/CI) so non-interactive runs never hang waiting for input.
 *
 * @param {string} message - Prompt text (the default is appended in brackets)
 * @param {string} defaultValue - Value used for a blank answer or no TTY
 * @returns {Promise<string>} The trimmed answer, or `defaultValue`
 */
export async function promptWithUser(message, defaultValue = "") {
  if (!process.stdin.isTTY) return defaultValue

  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  })

  const suffix = defaultValue ? ` [${defaultValue}]` : ""
  const answer = await rl.question(`${message}${suffix}: `)
  rl.close()

  return answer.trim() || defaultValue
}
