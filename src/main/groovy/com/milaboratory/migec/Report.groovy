package com.milaboratory.migec

def cli = new CliBuilder(usage: "Report [options] output_path'")

cli.c(longOpt: "checkout-path", args: 1, argName: "path", "Path to Checkout directory.")
cli.h(longOpt: "histogram-path", args: 1, argName: "path", "Path to Histogram directory.")
cli.a(longOpt: "assemble-path", args: 1, argName: "path", "Path to Assemble directory.")
cli.b(longOpt: "cdrblast-path", args: 1, argName: "path", "Path to CdrBlast directory.")
cli.f(longOpt: "cdrfinal-path", args: 1, argName: "path", "Path to FilterCdrBlastResults directory.")

def opt = cli.parse(args)

if (opt == null || opt.arguments().size() < 1) {
    println "[ERROR] Too few arguments provided"
    cli.usage()
    System.exit(-1)
}

if (opt.h) {
    println "[ERROR] Too few arguments provided"
    cli.usage()
    System.exit(0)
}

def checkoutPath = opt.c ?: "checkout/", histogramPath = opt.h ?: "histogram/",
    assemblePath = opt.a ?: "assemble/", cdrBlastPath = opt.b ?: "cdrblast/",
    cdrFinalPath = opt.f ?: "cdrfinal/", outputPath = opt.arguments()[0]

def rmdLines = new InputStreamReader(this.class.classLoader.getResourceAsStream("migec_summary.Rmd")).readLines().join("\n")

def replacePath = { String module, String path ->
    new File(path).exists() ?
            rmdLines.replace("__${module}__", path) :
            rmdLines.replace("\"__${module}__\"", "NULL")
}

rmdLines = replacePath("checkout", checkoutPath)
rmdLines = replacePath("histogram", histogramPath)
rmdLines = replacePath("assemble", assemblePath)
rmdLines = replacePath("cdrblast", cdrBlastPath)
rmdLines = replacePath("cdrfinal", cdrFinalPath)

if (new File(outputPath).isDirectory())
    outputPath += "/migec_summary.Rmd"
else if (outputPath.toLowerCase().endsWith(".html"))
    outputPath = outputPath[0..-6]

new File(outputPath).withPrintWriter { pw ->
    pw.println(rmdLines)
}

def scriptOutputPath = outputPath + ".generate.R"

new File(scriptOutputPath).withPrintWriter { pw ->
    pw.println(
            "require(\"rmarkdown\")\n" +
                    "render(\"$outputPath\")"
    )
}

def proc = "Rscript $scriptOutputPath".execute()

proc.in.eachLine {
    println(it)
}

proc.out.close()
proc.waitFor()

if (proc.exitValue()) {
    println "[ERROR] See log below:\n${proc.getErrorStream()}"
    println "[NOTE] Intermediate files were not deleted"
} else {
    new File(scriptOutputPath).delete()
    new File(outputPath).delete()
}