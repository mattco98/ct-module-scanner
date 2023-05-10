import java.io.File

val tests = mutableMapOf<String, Regex>()
val triggers = setOf(
    "chat",
    "actionBar",
    "worldLoad",
    "worldUnload",
    "clicked",
    "scrolled",
    "dragged",
    "soundPlay",
    "noteBlockPlay",
    "noteBlockChange",
    "tick",
    "step",
    "renderWorld",
    "renderOverlay",
    "renderPlayerList",
    "renderCrosshair",
    "renderDebug",
    "renderBossHealth",
    "renderHealth",
    "renderArmor",
    "renderFood",
    "renderMountHealth",
    "renderExperience",
    "renderHotbar",
    "renderAir",
    "renderPortal",
    "renderJumpBar",
    "renderChat",
    "renderHelmet",
    "renderHand",
    "renderScoreboard",
    "renderTitle",
    "drawBlockHighlight",
    "gameLoad",
    "gameUnload",
    "command",
    "guiOpened",
    "guiClosed",
    "playerJoined",
    "playerLeft",
    "pickupItem",
    "dropItem",
    "screenshotTaken",
    "messageSent",
    "itemTooltip",
    "playerInteract",
    "blockBreak",
    "entityDamage",
    "entityDeath",
    "guiDrawBackground",
    "guiRender",
    "guiKey",
    "guiMouseClick",
    "guiMouseRelease",
    "guiMouseDrag",
    "packetSent",
    "packetReceived",
    "serverConnect",
    "serverDisconnect",
    "chatComponentClicked",
    "chatComponentHovered",
    "renderEntity",
    "postRenderEntity",
    "renderTileEntity",
    "postRenderTileEntity",
    "postGuiRender",
    "preItemRender",
    "renderSlot",
    "renderItemIntoGui",
    "renderItemOverlayIntoGui",
    "renderSlotHighlight",
    "spawnParticle",
    "attackEntity",
    "hitBlock",
)

data class Counts(
    var total: Int,
    var numFiles: Int,
    var numReleases: Int,
    var numModules: Int
) {
    override fun toString() = "$total matches in $numFiles files, $numReleases releases, and $numModules modules"
}

fun main(args: Array<String>) {
    for (trigger in triggers)
        tests[trigger] = """register\(["']$trigger["']""".toRegex(RegexOption.IGNORE_CASE)

    val file = File("./src/main/resources/modules")
    require(file.exists() && file.isDirectory)

    val processes = mutableListOf<Pair<Process, File>>()

    file.walk().filter { it.name == "scripts.zip" }.forEach { f ->
        ProcessBuilder()
            .command("unzip", "-o", f.absolutePath, "-d", f.parentFile.absolutePath)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
            .also { processes.add(it to f) }
    }

    processes.forEach { (process, file) ->
        process.waitFor()
        file.delete()
    }

    val counts = tests.keys.associateWith { Counts(0, 0, 0, 0) }.toMutableMap()
    var totalModules = 0
    var emptyModules = 0

    for (module in file.listFiles()!!) {
        require(module.isDirectory)
        val moduleTests = mutableSetOf<String>()

        totalModules++
        if (module.listFiles()!!.isEmpty())
            emptyModules++

        for (release in module.listFiles()!!) {
            require(release.isDirectory)
            val releaseTests = mutableSetOf<String>()

            release.walk().filter { it.extension == "js" }.forEach {
                val text = it.readText()

                for ((test, regex) in tests) {
                    val occurrences = regex.findAll(text).count()
                    if (occurrences > 0) {
                        moduleTests.add(test)
                        releaseTests.add(test)
                        counts[test]!!.total += occurrences
                        counts[test]!!.numFiles += 1
                    }
                }
            }

            for (test in releaseTests)
                counts[test]!!.numReleases++
        }

        for (test in moduleTests)
            counts[test]!!.numModules++
    }

    println("Number of modules: $totalModules")
    println("Number of empty modules: $emptyModules\n")

    tests.keys.sortedByDescending { counts[it]!!.total }.forEach {
        println("$it: ${counts[it]}")
    }
}
