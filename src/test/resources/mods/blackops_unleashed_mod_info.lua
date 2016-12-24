name = "BlackOps Unleashed"
uid = "9e8ea941-c306-4751-b367-a11000000502"
version = 8
copyright = "Lt_hawkeye"
description = "Version 5.2. BlackOps Unleased Unitpack contains several new units and game changes. Have fun"
author = "Lt_hawkeye"
url = "http://forums.gaspowered.com/viewtopic.php?t=31172"
icon = "/mods/BlackOpsUnleashed/icons/yoda_icon.bmp"
selectable = true
enabled = true
exclusive = false
ui_only = false
requires = { "9e8ea941-c306-4751-b367-f00000000005" }
requiresNames = { }
conflicts = { }
before = { }
after = { }
mountpoints = {
    etc = "/etc",
    env = "/env",
    loc = '/loc',
    effects = '/effects',
    lua = '/lua',
    meshes = '/meshes',
    modules = '/modules',
    projectiles = '/projectiles',
    textures = '/textures',
    units = '/units'
}
hooks = {
    '/blackops'
}
