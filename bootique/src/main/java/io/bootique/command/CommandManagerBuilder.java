package io.bootique.command;

import io.bootique.BootiqueException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @since 0.25
 */
public class CommandManagerBuilder<T extends CommandManagerBuilder<T>> {

    protected Collection<Command> commands;
    protected Command helpCommand;
    protected Optional<Command> defaultCommand;

    public CommandManagerBuilder(Collection<Command> commands) {
        this.commands = commands;
    }

    public T helpCommand(Command helpCommand) {
        this.helpCommand = helpCommand;
        return (T) this;
    }

    public T defaultCommand(Optional<Command> defaultCommand) {
        this.defaultCommand = defaultCommand;
        return (T) this;
    }

    public CommandManager build() {
        return new DefaultCommandManager(buildCommandMap());
    }

    protected Map<String, ManagedCommand> buildCommandMap() {
        Map<String, ManagedCommand> commandMap = new HashMap<>();

        loadCommands(commandMap);
        loadHelpCommand(commandMap);
        loadDefaultCommand(commandMap);
        return commandMap;
    }

    protected void loadCommands(Map<String, ManagedCommand> commandMap) {
        commands.forEach(c -> addCommandNoOverride(commandMap, c));
    }

    protected void loadHelpCommand(Map<String, ManagedCommand> commandMap) {
        addCommandNoOverride(commandMap, ManagedCommand.builder(helpCommand).asHelp().build());
    }

    protected void loadDefaultCommand(Map<String, ManagedCommand> commandMap) {
        // as default command can serve as an ad-hoc alias for another command, it is allowed to override other
        // commands with the same name with no complaints
        defaultCommand.ifPresent(c -> addCommand(commandMap, ManagedCommand.builder(c).asDefault().build()));
    }

    protected ManagedCommand addCommand(Map<String, ManagedCommand> commandMap, ManagedCommand managedCommand) {

        Command command = managedCommand.getCommand();
        String name = command.getMetadata().getName();
        return commandMap.put(name, managedCommand);
    }

    protected void addCommandNoOverride(Map<String, ManagedCommand> commandMap, Command command) {
        addCommandNoOverride(commandMap, ManagedCommand.forCommand(command));
    }

    protected void addCommandNoOverride(Map<String, ManagedCommand> commandMap, ManagedCommand managedCommand) {

        ManagedCommand existing = addCommand(commandMap, managedCommand);

        // complain on dupes
        if (existing != null && existing.getCommand() != managedCommand.getCommand()) {
            String c1 = existing.getCommand().getClass().getName();
            String c2 = managedCommand.getCommand().getClass().getName();

            String message = String.format("More than one DI command named '%s'. Conflicting types: %s, %s.",
                    managedCommand.getCommand().getMetadata().getName(), c1, c2);
            throw new BootiqueException(1, message);
        }
    }
}
