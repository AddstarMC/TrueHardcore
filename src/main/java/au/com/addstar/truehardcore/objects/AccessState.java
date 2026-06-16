/*
 * TrueHardcore
 * Copyright (C) 2013 - 2024  AddstarMC <copyright at addstar dot com dot au>
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

package au.com.addstar.truehardcore.objects;

/**
 * Represents a player's current eligibility to play a hardcore world.
 *
 * <p>Computed read-only by {@code TrueHardcore.getAccessState} and surfaced via the
 * {@code canplay} / {@code access_reason} placeholders for the lobby hologram. Each
 * constant carries a default human-readable reason; {@link #COOLDOWN} expects the
 * caller to append the remaining time.
 */
public enum AccessState {
    ALLOWED("Allowed"),
    NOT_WHITELISTED("Not whitelisted"),
    ALT("Alt account"),
    ROLLBACK("Rollback in progress"),
    COOLDOWN("On cooldown"),
    DISABLED("Game disabled"),
    IN_GAME("Already playing");

    private final String reason;

    AccessState(String reason) {
        this.reason = reason;
    }

    /**
     * @return the default human-readable reason text for this state
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return true if the player is allowed to play
     */
    public boolean canPlay() {
        return this == ALLOWED;
    }
}
