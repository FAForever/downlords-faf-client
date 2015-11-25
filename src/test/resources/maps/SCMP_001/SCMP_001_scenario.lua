version = 3
ScenarioInfo = {
    name = 'Burial Mounds',
    description = '<LOC SCMP_001_Description>Initial scans of the planet revealed evenly spaced hills, leading some to believe that the hills were ancient alien burial mounds. Subsequent scans disproved this theory, but many believe that the old Earth Empire actually found alien remains, spirited them away and then faked the scans as part of the cover-up.',
    type = 'skirmish',
    starts = true,
    preview = '',
    size = { 1024, 1024 },
    map = '/maps/SCMP_001/SCMP_001.scmap',
    save = '/maps/SCMP_001/SCMP_001_save.lua',
    script = '/maps/SCMP_001/SCMP_001_script.lua',
    norushradius = 90.000000,
    norushoffsetX_ARMY_1 = 15.000000,
    norushoffsetY_ARMY_1 = 0.000000,
    norushoffsetX_ARMY_2 = 10.000000,
    norushoffsetY_ARMY_2 = -15.000000,
    norushoffsetX_ARMY_3 = 0.000000,
    norushoffsetY_ARMY_3 = 0.000000,
    norushoffsetX_ARMY_4 = 0.000000,
    norushoffsetY_ARMY_4 = 0.000000,
    norushoffsetX_ARMY_5 = 0.000000,
    norushoffsetY_ARMY_5 = 10.000000,
    norushoffsetX_ARMY_6 = 2.000000,
    norushoffsetY_ARMY_6 = -12.000000,
    norushoffsetX_ARMY_7 = 0.000000,
    norushoffsetY_ARMY_7 = 0.000000,
    norushoffsetX_ARMY_8 = 0.000000,
    norushoffsetY_ARMY_8 = -5.000000,
    Configurations = {
        ['standard'] = {
            teams = {
                { name = 'FFA', armies = { 'ARMY_1', 'ARMY_2', 'ARMY_3', 'ARMY_4', 'ARMY_5', 'ARMY_6', 'ARMY_7', 'ARMY_8', } },
            },
            customprops = {
                ['ExtraArmies'] = STRING('ARMY_9 NEUTRAL_CIVILIAN'),
            },
        },
    }
}
