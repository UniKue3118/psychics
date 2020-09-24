/*
 * Copyright (c) 2020 Noonmaru
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 */

package com.github.noonmaru.psychics.plugin

import com.github.noonmaru.kommand.kommand
import com.github.noonmaru.psychics.PsychicManager
import com.github.noonmaru.psychics.Psychics
import com.github.noonmaru.psychics.command.CommandPsychic
import com.github.noonmaru.tap.event.EntityEventManager
import com.github.noonmaru.tap.fake.FakeEntityServer
import com.github.noonmaru.tap.util.GitHubSupport
import com.github.noonmaru.tap.util.UpToDateException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * @author Noonmaru
 */
class PsychicPlugin : JavaPlugin() {

    lateinit var fakeEntityServer: FakeEntityServer
        private set

    lateinit var entityEventManager: EntityEventManager
        private set

    lateinit var psychicManager: PsychicManager
        private set

    override fun onEnable() {
        loadModules()
        setupCommands()
        registerPlayers()
        Psychics.initialize(this, logger, psychicManager, fakeEntityServer)
    }

    private fun loadModules() {
        fakeEntityServer = FakeEntityServer.create(this)
        entityEventManager = EntityEventManager(this)
        psychicManager = PsychicManager(
            this,
            logger,
            File(dataFolder, "abilities"),
            File(dataFolder, "psychics"),
            File(dataFolder, "espers")
        )

        psychicManager.loadAbilities()
        psychicManager.loadPsychics()

        server.apply {
            pluginManager.registerEvents(
                EventListener(
                    psychicManager,
                    fakeEntityServer
                ), this@PsychicPlugin
            )
            scheduler.runTaskTimer(this@PsychicPlugin, SchedulerTask(psychicManager, fakeEntityServer), 0L, 1L)
        }
    }

    private fun registerPlayers() {
        for (player in server.onlinePlayers) {
            fakeEntityServer.addPlayer(player)
            psychicManager.addPlayer(player)
        }
    }

    private fun setupCommands() {
        CommandPsychic.initModule(this, this.psychicManager)
        kommand {
            register("psychics") {
                CommandPsychic.register(this)
            }
        }
    }

    override fun onDisable() {
        if (this::psychicManager.isInitialized) {
            psychicManager.unload()
        }
    }

    // Update example
    internal fun update(sender: CommandSender) {
        sender.sendMessage("Attempt to update.")
        update {
            onSuccess { url ->
                sender.sendMessage("Updated successfully. Applies after the server restarts.")
                sender.sendMessage(url)
            }
            onFailure { t ->
                if (t is UpToDateException) sender.sendMessage("Up to date!")
                else {
                    sender.sendMessage("Update failed. Check the console.")
                    t.printStackTrace()
                }
            }
        }
    }

    private fun update(callback: (Result<String>.() -> Unit)? = null) {
        GlobalScope.launch {
            val file = file
            val updateFile = File(file.parentFile, "update/${file.name}")
            GitHubSupport.downloadUpdate(updateFile, "noonmaru", "psychics", description.version, callback)
        }
    }
}