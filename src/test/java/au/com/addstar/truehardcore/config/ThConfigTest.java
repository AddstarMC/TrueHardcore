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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 10/03/2020.
 */

public class ThConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    File file;

    @Before
    public void setup(){
        try {
            file = folder.newFile("test.yml");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void configTest() {
        ThConfig config = new ThConfig(file,"Test");
        config.debugEnabled = true;
        config.save();

        ThConfig newConfig = new ThConfig(file,"Test");
        newConfig.load();
        assertEquals(config.debugEnabled, newConfig.debugEnabled);


    }

}