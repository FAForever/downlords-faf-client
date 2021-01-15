version = 3 -- Lua Version. Dont touch this
ScenarioInfo = {
    name = "Palaneum",
    description = "Made by Chosen. This is not the final version. Fixed and better map preview images coming later, including many other things. Perhaps the best experience you get when you set game options: Interface - Display World Border OFF and Video - Render Sky is set ON and Fidelity is set to High and Bloom Render is set ON. If you wish more new maps, you can support me. https://streamlabs.com/Chosen_FAF",
    preview = '',
    map_version = 2,
    type = 'skirmish',
    starts = true,
    size = {256, 256},
    reclaim = {4413.491, 11317.42},
    map = '/maps/palaneum.v0002/Palaneum.scmap',
    save = '/maps/palaneum.v0002/Palaneum_save.lua',
    script = '/maps/palaneum.v0002/Palaneum_script.lua',
    norushradius = 40,
    Configurations = {
        ['standard'] = {
            teams = {
                {
                    name = 'FFA',
                    armies = {'ARMY_1', 'ARMY_2'}
                },
            },
            customprops = {
                ['ExtraArmies'] = STRING( 'ARMY_17 NEUTRAL_CIVILIAN' ),
            },
        },
    },
}
