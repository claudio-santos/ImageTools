package main;

import java.util.Stack;

/**
 *
 * @author Claudio Santos
 */
public class UndoRedo {

    private final Stack<MyImage> undo;
    private final Stack<MyImage> redo;

    public UndoRedo() {
        undo = new Stack<>();
        redo = new Stack<>();
    }

    public void action(MyImage action) {
        undo.add(action);
        redo.clear();
    }

    public MyImage undo() {
        if (undo.size() > 1) {
            redo.push(undo.pop());
            return undo.peek();
        }
        return null;
    }

    public MyImage redo() {
        if (!redo.isEmpty()) {
            undo.push(redo.pop());
            return undo.peek();
        }
        return null;
    }

}
