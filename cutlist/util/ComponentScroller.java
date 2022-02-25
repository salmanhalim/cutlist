package cutlist.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Timer;

public enum ComponentScroller {
    INSTANCE;

    protected static boolean s_scrolling = false;

    protected static double snap(double percent) {
        return 1;
    }

    protected static double linear(double percent) {
        return percent;
    }

    protected static double easeInCubic(double percent) {
        return Math.pow(percent, 3);
    }

    protected static double easeOutCubic(double percent) {
        return 1 - easeInCubic(1 - percent);
    }

    protected static double easeInOutCubic(double percent) {
        return percent < 0.5
                ? easeInCubic(percent * 2) / 2
                : easeInCubic(percent * -2 + 2) / -2 + 1;
    }

    protected JScrollBar m_vertBar;
    protected int        m_target;
    protected Timer      m_timer;

    public void scrollVertically(JScrollPane scrollPane, int target) {
        if (s_scrolling) {
            // Just jump straight to the finish.
            m_timer.stop();
            m_vertBar.setValue(m_target);

            s_scrolling = false;
        }

        m_target  = target;
        m_vertBar = scrollPane.getVerticalScrollBar();

        final int start               = m_vertBar.getValue();
        final int delta               = target - start;
        final int msBetweenIterations = 10;

        // System.out.println("-=-=-=-=-= scrollVertically " + "[" + "start: " + start + "], [" + "target: " + target + "], [" + "delta: " + delta + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        s_scrolling = true;

        m_timer = new Timer(msBetweenIterations, new ActionListener() {
            int currentIteration = 0;

            final long animationTime        = 500;                                      // Milliseconds
            final long nsBetweenIterations  = msBetweenIterations * 1000000;            // Nanoseconds
            final long startTime            = System.nanoTime() - nsBetweenIterations;  // Make the animation moves on the first iteration
            final long targetCompletionTime = startTime + animationTime * 1000000;
            final long targetElapsedTime    = targetCompletionTime - startTime;

            @Override
            public void actionPerformed(ActionEvent e) {
                long   timeSinceStart  = System.nanoTime() - startTime;
                double percentComplete = Math.min(1.0, (double) timeSinceStart / targetElapsedTime);

                double factor = linear(percentComplete);

                int newValue = (int) Math.round(start + delta * factor);

                // System.out.println("-=-=-=-=-= actionPerformed newValue: " + (newValue) + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

                m_vertBar.setValue(newValue);

                if (timeSinceStart >= targetElapsedTime) {
                    ((Timer) e.getSource()).stop();

                    s_scrolling = false;
                }
            }
        });

        m_timer.setInitialDelay(0);
        m_timer.start();
    }
}
