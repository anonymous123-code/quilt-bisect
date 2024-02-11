# Quilt Bisect

A mod helping in finding mods responsible for issues using an extended binary search algorithm.

## Usage

- This project uses Loader Plugins, `-Dloader.experimental.allow_loading_plugins=true` needs to be added to your JVM arguments.
- Move the mods you want to test into a directory called `modsToBisect` in your minecraft folder
- If your game crashes, a dialog will open asking if you want to bisect, else click the start bisect button at the top of your screen, and you will be prompted to give your issue a name. 
- The game will automatically restart. Try to reproduce the issues you had before.
  - If everything is fine, click the `No Issue` button
  - If you have an issue click the `Manual Issue` button, and select the issue you are having or create a new one
    - Note that it is recommended to keep the number of issues in one bisect down, and instead run multiples with previous fixes applied (todo: maybe remove this?)
- (todo) once an issue is isolated, bisect will verify the solution(s) and continue bisecting the other issues
- once bisect is done, it will provide a summary of issues and solutions encountered. Please treat those with care though, [due to the way bisect works](#loading-the-right-mod-set)

## How does it work
### Automatically mark things as working
When starting a new bisect, you can configure bisect to
- automatically join a world, server, realm, or the last joined one (Note that realms rely on ids and not on names)
- automatically accept a mod set as working after a given time
  - The time is in 1/20 seconds, and will start on world join if auto join is active, else when the title screen shows. If in single player, it will also wait for enough ticks to pass
- automatically run commands or sending chat messages after auto joining.
  - It will be treated as if you sent them from chat, and also work with client side commands
- disable world saving (TODO)
  - This is to help reproduction of issues where the state of the world is relevant. No single player world will be changed after a relaunch if this is active

### Handling crashes
Being a loader plugin, bisect hooks into the Quilt's loader process at an incredibly early stage.
There, it can create a new process that is actually running the game, and wait for that process to end.
After that bisect checks if minecraft crashed by searching for a new crash log, copies the latest.log, and updates `config/bisect/active_bisect.json`.
(if no bisect is active and it crashed it asks if a new one should be started)
If minecraft didn't quit normally (either through a crash or through the bisect menu) and a bisect is active, the parent process will restart the game.
[Behind the scenes](src/main/java/io/github/anonymous123_code/quilt_bisect/plugin/BisectPluginProcessManager.java) there is a lot more stuff going on making sure that for example killing the parent or child process makes them  behave as expected.

### Detecting issues
Bisect will
- [Handle crashes](#handling-crashes)
- (TODO) Check for log messages using regexes (if possible)
- Rely on user input

### Selecting the next mod set to test
Bisect works using an extended version of binary search:
- First, select the smallest mod set with an (unfixed) issue
- For each section of that mod set(see last row)
  - If a section simply includes one mod, that mod is required and no more work needs to be done on that section
  - Divide that section into half
  - Check those halves
    - if one has an issue, it becomes a part of the issue mod set in the next iteration
    - if all halves are working this means that in both halves are mods required for the issue to arise
      - => store those halves as separate sections and bisect those like in step 3
- if all sections only contain one element, a minimal reproduction has been isolated. Mark the corresponding issue as fixed with the resulting mod set and debug the next issue, with one of the current mods removed
- Once all of this is done, prompt the user to select which mods are to be removed

### Loading the right mod set
One of the main problems such a bisect algorithm faces are dependencies.
Ideally, a bisect method would be aware of dependencies and bisect based on those.
However, this would immensely inflate the complexity of the algorithm, and this project is complex enough right now.

Instead, Quilt Bisect simply ignores dependency in its search algorithm and provides all mods to quilt loader.
However, it only force-loads mods in the mod set (this is what happens to mods in your mods folder) and provides the other mods as optional mods which are loaded when a dependency requires it.
While solving the dependency issue, it has a few implications:
- If required, a mod not in the mod set still can be loaded
- This means that it can happen that when bisect identifies a minimal reproduction, that reproduction set actually only includes a mod which depends on the real culprit
- This means that the mod set *can not show* which mod is at fault and that before blaming a mod some better tests must occur

## Development

For testing, I also recommend [Quilt Crasher](https://github.com/anonymous123-code/quilt-crasher), which I developed to enable testing of this kind of mod.

## Licensing

While the code is ARR, this is only to prevent ports to other mod loaders or low-effort copies. Feel free to copy parts of the code for your mod. If your mod does similar things to this mod, credit me. You can also ask for me for permission. If it's not a port to another mod loader, I'll likely agree.

---

`PluginLogger`, `MinecraftServerMixin` and `GracefulTerminator` were mostly taken from https://github.com/comp500/ModVote.
`BisectPlugin` also contains large parts of `ModvotePlugin` from that repo.
All of these are under the [MIT license](Modvote-License) (by comp500)
