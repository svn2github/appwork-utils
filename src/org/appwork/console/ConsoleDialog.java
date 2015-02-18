package org.appwork.console;

import java.io.IOException;

import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
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

        this.console = AbstractConsole.newInstance();
        if (this.console == null) {
            throw new RuntimeException("No Console Available!");
        }
        this.title = string;
    }

    public void start() {
        this.stdBefore = false;
        this.errBefore = false;
        try {
            this.stdBefore = Application.STD_OUT.setBufferEnabled(true);
            this.errBefore = Application.ERR_OUT.setBufferEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
            // cannot happen for parameter=true;
        }
        this.console.println("|---------------------------Headless Information-------------------------------");
        this.console.println("|\t" + this.title);

    }

    public void end() {
        this.console.println("|------------------------------------------------------------------------------");

        try {
            Application.STD_OUT.setBufferEnabled(this.stdBefore);
            Application.STD_OUT.flush();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            Application.ERR_OUT.setBufferEnabled(this.errBefore);
            Application.ERR_OUT.flush();
        } catch (Throwable e) {
            e.printStackTrace();

        }

    }

    public void println(String string) {
        this.console.println("|\t" + string);
    }

    public void waitYesOrNo(int flags, String yes, String no) throws DialogCanceledException, DialogClosedException {
        if ((flags & UIOManager.BUTTONS_HIDE_OK) != 0 || (flags & UIOManager.BUTTONS_HIDE_CANCEL) != 0) {
            this.waitToContinue((flags & UIOManager.BUTTONS_HIDE_OK) != 0 ? yes : no);

            if ((flags & UIOManager.BUTTONS_HIDE_OK) != 0) {

            } else if ((flags & UIOManager.BUTTONS_HIDE_CANCEL) != 0) {
                throw new DialogCanceledException(Dialog.RETURN_CANCEL);

            } else {
                throw new DialogClosedException(Dialog.RETURN_CLOSED);
            }

        } else {
            while (true) {
                this.println("Enter y -> " + yes);
                this.println("Enter n -> " + no);

                String c;

                c = this.console.readLine();

                if (c.trim().equalsIgnoreCase("y")) {

                    return;
                } else if (c.trim().equalsIgnoreCase("n")) {
                    throw new DialogCanceledException(Dialog.RETURN_CANCEL);
                }
            }

        }

    }

    public void printLines(String stackTrace) {
        for (String l : Regex.getLines(stackTrace)) {
            this.println(l);
        }
    }

    public void waitToContinue() {
        this.waitToContinue("continue");

    }

    public void waitToContinue(String string) {
        if (string == null) {
            string = "continue";
        }
        this.println("Press Enter to " + string);
        this.console.readLine();
    }

    public void print(String string) {
        this.console.print(string);
    }

    /**
     * @param string
     * @return
     */
    public String ask(String string) {
        this.println(string);
        return this.console.readLine();
    }

    public String askHidden(String string) {
        this.println(string);
        return this.console.readPassword();
    }

    /**
     * @param string
     * @param string2
     * @param e
     * @return
     */
    public static boolean showExceptionDialog(String title, String message, Throwable e) {
        if (!Application.isHeadless()) {
            return false;
        }
        synchronized (AbstractConsole.LOCK) {

            ConsoleDialog cd = new ConsoleDialog("Exception occured");
            cd.start();
            try {
                cd.println(title);
                cd.printLines(message);
                cd.printLines(Exceptions.getStackTrace(e));
                cd.waitToContinue();

                return true;
            } finally {
                cd.end();
            }

        }
    }
}
