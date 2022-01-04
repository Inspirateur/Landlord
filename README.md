# Landlord Minecraft Plug-In
A Minecraft Plugin for claiming and protecting land against Mob griefing, Player griefing and PvP in exchange for in-game resources.

## Features
To claim a land, the player defines a 3D rectangle to protect using `/corner1` and `/corner2` at the location of 2 opposites corners.  
Particles will then highlight the selected area and the player can use `/protect mobGrief|playerGrief|PVP` to add a protection to the area.

The cost necessary to protect an area will scale with its volume and change depending on the type of protection required.

The Player griefing protection will stop other players from destroying or adding new blocks but they will still be able to interact with doors, buttons, chests and so on.
`/add <Player>` can be used to add exceptions to the Player griefing protection.
