import kotlinx.cinterop.CPointer
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import platform.posix.FILE
import platform.posix.NULL
import platform.posix.exit
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen
import platform.posix.printf

fun main(args: Array<String>) {
    val parser = ArgParser("keystore")
    val inputFile by parser.option(ArgType.String, shortName = "e", description = "Export from this keystore").required()
    val outputDir by parser.option(ArgType.String, shortName = "o", description = "Output directory").default("/tmp/certs")
    val importTo by parser.option(ArgType.String, shortName = "i", description = "Import to this keystore")
    parser.parse(args)

    execute("mkdir -p $outputDir")

    val numberOfOldCerts = execute("keytool -list -keystore $importTo -storepass changeit | grep entries | grep -Eo \"\\d{1,}\"")
    println("Exporting $numberOfOldCerts certs")

    val aliases = execute("keytool -list -keystore $inputFile -storepass changeit | grep trustedCertEntry | grep -Eo \"^[^,]*\" | sort -u | sed 's/ /\\\\ /g'").split("\n")
    var numberExported = 0
    aliases.forEach {
        val command = "keytool -export -storepass changeit -alias $it -keystore \"$inputFile\" -file /tmp/certs/$it.crt"
        println(command)
        execute(command)
        numberExported++
    }

    var numberImported = 0
    importTo.also { file ->
        aliases.forEach {
            val command = "echo yes | keytool -import -alias $it -keystore $file -storepass changeit -file /tmp/certs/$it.crt"
            println(command)
            execute(command)
            numberImported++
        }
    }

    println("Number of certs exported from $inputFile: $numberExported")
    println("Number of certs imported into $importTo: $numberImported")

    importTo.also {
        println("Cleaning up $outputDir")
        execute("rm -r $outputDir")
    }
}

fun execute(command: String): String {
    val fp: CPointer<FILE>? = popen(command, "r")
    val buffer = ByteArray(4096)
    val returnString = StringBuilder()

    if (fp == NULL) {
        printf("Failed to run command\n")
        exit(1)
    }

    var scan = fgets(buffer.refTo(0), buffer.size, fp)
    if (scan != null) {
        while (scan != NULL) {
            returnString.append(scan!!.toKString())
            scan = fgets(buffer.refTo(0), buffer.size, fp)
        }
    }

    pclose(fp)
    return returnString.toString().trim()
}

