import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val url = args.firstOrNull() ?: run {
    println("Argument: [url] are required")
    exitProcess(1)
  }
  val redirect = executeCommand("curl -G '$url'")
  val firstMatched = Regex(""".*<a.+href="(.+)">.+</a>.*""").find(redirect)?.destructured?.component1() ?: run {
    println("Failed")
    exitProcess(1)
  }
  val result = Regex("""(https://.+\.bilibili\.com/.+)\?""").find(firstMatched)?.destructured?.component1() ?: run {
    println("Failed")
    exitProcess(1)
  }
  executeCommand("echo '$result' | pbcopy")
}

fun executeCommand(
  command: String,
  trim: Boolean = true,
  redirectStderr: Boolean = true
): String {
  val commandToExecute = if (redirectStderr) "$command 2>&1" else command
  val fp = popen(commandToExecute, "r") ?: error("Failed to run command: $command")

  val stdout = buildString {
    val buffer = ByteArray(4096)
    while (true) {
      val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
      append(input.toKString())
    }
  }

  val status = pclose(fp)
  if (status != 0) {
    error("Command `$command` failed with status $status${if (redirectStderr) ": $stdout" else ""}")
  }

  return if (trim) stdout.trim() else stdout
}
