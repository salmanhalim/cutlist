package cutlist.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArgsProcessor {
    protected String[] m_args;

    protected Map<String, String> m_arguments      = new HashMap<>();
    protected List<String>        m_extraArguments = new ArrayList<>();

    private String m_argumentName;

    public ArgsProcessor(String args[]) {
        m_args = args;

        process();
    }

    public String[] getArgs() {
        return m_args;
    }

    public void setArgs(String[] val) {
        m_args = val;
    }

    public void process() {
        Arrays.stream(m_args).forEach(arg -> {
            if (arg.startsWith("--")) {
                if (m_argumentName != null) {
                    m_arguments.put(m_argumentName, "1");
                }

                m_argumentName = arg.substring(2);
            } else if (arg.startsWith("-")) {
                if (m_argumentName != null) {
                    m_arguments.put(m_argumentName, "1");
                }

                m_argumentName = arg.substring(1);
            } else if (m_argumentName != null) {
                m_arguments.put(m_argumentName, arg);

                m_argumentName = null;
            } else {
                m_extraArguments.add(arg);
            }
        });

        // Ended on a boolean argument.
        if (m_argumentName != null) {
            m_arguments.put(m_argumentName, "1");
        }

        System.out.println("-=-=-=-=-= m_arguments: " + m_arguments + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
        System.out.println("-=-=-=-=-= m_extraArguments: " + m_extraArguments + " (" + new java.util.Date() + " " + new Exception().getStackTrace()[0] + ")"); // SALMAN: remove
    }

    public String get(String arg) {
        return m_arguments.get(arg);
    }

    public Optional<Integer> getInt(String arg) {
        String value = m_arguments.get(arg);

        return Optional.ofNullable(value == null ? null : Integer.valueOf(m_arguments.get(arg)));
    }

    public boolean getBoolean(String arg) {
        return "1".equals(m_arguments.get(arg));
    }

    public Optional<String> getExtraArguments() {
        return m_extraArguments.size() > 0 ? Optional.of(String.join(" ", m_extraArguments)) : Optional.empty();
    }
}
