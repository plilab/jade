

### Setup for about
set -l main_about_subcommands 'build-info loggers generate-completion'
complete -c main -f -n __fish_use_subcommand -a about -d 'Commands about `jade`'

## Options for about
complete -c main -n "__fish_seen_subcommand_from about" -s h -l help -d 'Show this message and exit'


### Setup for build-info
complete -c main -f -n "__fish_seen_subcommand_from about; and not __fish_seen_subcommand_from $main_about_subcommands" -a build-info -d 'Show information about how `jade` was built'

## Options for build-info
complete -c main -n "__fish_seen_subcommand_from build-info" -s h -l help -d 'Show this message and exit'


### Setup for loggers
complete -c main -f -n "__fish_seen_subcommand_from about; and not __fish_seen_subcommand_from $main_about_subcommands" -a loggers -d 'List available loggers'

## Options for loggers
complete -c main -n "__fish_seen_subcommand_from loggers" -l test -d 'Send test messages to all loggers'
complete -c main -n "__fish_seen_subcommand_from loggers" -s h -l help -d 'Show this message and exit'


### Setup for generate-completion
complete -c main -f -n "__fish_seen_subcommand_from about; and not __fish_seen_subcommand_from $main_about_subcommands" -a generate-completion -d 'Generate a tab-complete script for the given shell'

## Options for generate-completion
complete -c main -n "__fish_seen_subcommand_from generate-completion" -s h -l help -d 'Show this message and exit'

## Arguments for generate-completion
complete -c main -n "__fish_seen_subcommand_from generate-completion" -fa "bash zsh fish"

