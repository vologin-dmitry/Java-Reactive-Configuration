package uk.dsx.reactiveconfig

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.dsx.reactiveconfig.interfaces.ConfigSource
import uk.dsx.reactiveconfig.interfaces.RawProperty
import java.io.File
import java.nio.file.*
import java.util.*
import kotlin.concurrent.thread

class FileConfigSource(private val directory: Path, private val fileName: String) :
    ConfigSource {
    private var config: List<String>? = null
    private val file = Paths.get(directory.toAbsolutePath().toString() + File.separator + fileName)
    private val watchService: WatchService? = FileSystems.getDefault().newWatchService()
    private var key: WatchKey? = null

    init {
        directory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
    }

    @ObsoleteCoroutinesApi
    override suspend fun subscribe(dataStream: Channel<RawProperty>) {
        thread(true)
        {
            GlobalScope.launch()
            {
                try {
                    for (string in Files.readAllLines(file)) {
                        config
                        val splitted = string.split(' ')
                        dataStream.send(
                            RawString(
                                StringDescription(splitted[0]),
                                splitted[1]
                            )
                        )
                    }
                    while (true) {
                        var updated: List<String>
                        try {
                            updated = Files.readAllLines(file)
                            key = watchService!!.take();
                        } catch (e: NoSuchFileException) {
                            delay(10)
                            updated = Files.readAllLines(file)
                        }
                        for (string in giveMeListOfChanges(config, updated)) {
                            val splitted = string.split(' ')
                            dataStream.send(
                                RawString(
                                    StringDescription(
                                        splitted[0]
                                    ), splitted[1]
                                )
                            )
                        }
                        key!!.reset();
                    }
                } catch (e: Exception) {
                    e.printStackTrace();
                }
            }
        }
    }

    private suspend fun giveMeListOfChanges(previous: List<String>?, updated: List<String>): List<String> {
        val changes: LinkedList<String> = LinkedList()
        if (previous == null) {
            config = updated
            return updated
        }
        for (upd in updated) {
            if (!previous.contains(upd)) {
                changes.add(upd)
            }
        }
        config = updated
        return changes
    }
}