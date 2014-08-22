package org.appwork.console;

import java.io.IOException;

import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class ConsoleDialog {
    private AbstractConsole console;
    private boolean         stdBefore;
    private boolean         errBefore;
    private String          title;

    public ConsoleDialog(String string) {
        console = AbstractConsole.newInstance();
        if (console == null) { throw new RuntimeException("No Console Available!"); }
        this.title = string;

    }

    public void start() {
        stdBefore = false;
        errBefore = false;
        try {
            stdBefore = Application.STD_OUT.setBufferEnabled(true);
            errBefore = Application.ERR_OUT.setBufferEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
            // cannot happen for parameter=true;
        }
        console.println("|---------------------------Headless Information-------------------------------");
        console.println("|\t" + title);

    }

    public void end() {
        console.println("|------------------------------------------------------------------------------");

        try {
            Application.STD_OUT.setBufferEnabled(stdBefore);
            Application.STD_OUT.flush();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            Application.ERR_OUT.setBufferEnabled(errBefore);
            Application.ERR_OUT.flush();
        } catch (Throwable e) {
            e.printStackTrace();

        }

    }

    public void println(String string) {
        console.println("|\t" + string);
    }

    public void waitYesOrNo(int flags, String yes, String no) throws DialogCanceledException, DialogClosedException {
        if ((flags & UIOManager.BUTTONS_HIDE_OK) != 0 || (flags & UIOManager.BUTTONS_HIDE_CANCEL) != 0) {
            waitToContinue(((flags & UIOManager.BUTTONS_HIDE_OK) != 0) ? yes : no);

            if ((flags & UIOManager.BUTTONS_HIDE_OK) != 0) {

            } else if ((flags & UIOManager.BUTTONS_HIDE_CANCEL) != 0) {
                throw new DialogCanceledException(Dialog.RETURN_CANCEL);

            } else {
                throw new DialogClosedException(Dialog.RETURN_CLOSED);
            }

        } else {
            while (true) {
                println("Enter y -> " + yes);
                println("Enter n -> " + no);

                String c;

                c = console.readLine();

                if (c.trim().equalsIgnoreCase("y")) {

                    return;
                } else if (c.trim().equalsIgnoreCase("n")) { throw new DialogCanceledException(Dialog.RETURN_CANCEL); }
            }

        }

    }

    public void printLines(String stackTrace) {
        for (String l : Regex.getLines(stackTrace)) {
            println(l);
        }
    }

    public void waitToContinue() {
        waitToContinue("vontinue");

    }

    public void waitToContinue(String string) {
        println("Press Enter to " + string);

        console.readLine();

    }

    public void print(String string) {
        console.print(string);
    }

    /**
     * @param string
     * @return
     */
    public String ask(String string) {
        println(string);

        return console.readLine();
    }
}
