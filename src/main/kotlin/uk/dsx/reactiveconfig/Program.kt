package uk.dsx.reactiveconfig

import kotlinx.coroutines.channels.Channel
import uk.dsx.reactiveconfig.interfaces.RawProperty
import java.nio.file.Paths


suspend fun main() {
    val path = Paths.get(".", "src", "main", "kotlin", "uk", "dsx", "reactiveconfig", "configsources")
    val cfg = FileConfigSource(path, "config")
    val d = Channel<RawProperty>()
    cfg.subscribe(d)
    while (true) {
        for (c in d) {
            println(c.value as String)
        }
    }
}