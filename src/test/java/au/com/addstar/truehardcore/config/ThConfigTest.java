/*
 * TrueHardcore
 * Copyright (C) 2013 - 2020  AddstarMC <copyright at addstar dot com dot au>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package au.com.addstar.truehardcore.config;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created for the Charlton IT Project.
 * Created by narimm  on 10/03/2020.
 */

public class ThConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    File file;
    File lobbyWorldFile;
    File hardcoreWorldFile;
    static ServerMock server = MockBukkit.mock();
    WorldMock lobby;
    WorldMock hardcore;


    @Before
    public void setup() {
        lobby = new WorldMock();
        hardcore = new WorldMock();
        lobby.setName("lobby");
        hardcore.setName("hardcore");
        server.addWorld(lobby);
        server.addWorld(hardcore);

        try {
            file = folder.newFile("test.yml");
            lobbyWorldFile = folder.newFile(lobby.getName() + ".yml");
            hardcoreWorldFile = folder.newFile(hardcore.getName() + ".yml");
        } catch (IOException e) {
            Logger.getAnonymousLogger().info(e.getMessage());
        }
    }

    @Test
    public void configTest() {
        ThConfig config = new ThConfig(file, "Test");
        HardcoreWorldConfig worldConfig
              = new HardcoreWorldConfig(hardcoreWorldFile, "TrueHardCore", hardcore.getName());
        worldConfig.save();
        config.worlds.add(hardcore.getName());
        config.debugEnabled = true;
        config.save();
        ThConfig newConfig = new ThConfig(file, "Test");
        newConfig.load();
        assertEquals(config.debugEnabled, newConfig.debugEnabled);
        assertTrue(newConfig.worlds.contains("hardcore"));
    }

}