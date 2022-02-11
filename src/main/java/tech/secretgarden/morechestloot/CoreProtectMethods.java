package tech.secretgarden.morechestloot;

import net.coreprotect.CoreProtectAPI;

import java.util.List;

public class CoreProtectMethods {
    private final CoreProtectAPI CoreProtect = new CoreProtectAPI();
    public boolean actionLookup(List<String[]> lookup) {
        int i = 1;
        for (String[] value : lookup) {
            //gets every result from CP api blockLookup method.
            CoreProtectAPI.ParseResult result = CoreProtect.parseResult(value);
            int action = result.getActionId();
            //actions are either placed, broken, or interact.
            if (action == 1 || action == 0) {
                i = i + 1;
            }
        }
        return i == 1;
    }
}
