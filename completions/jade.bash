
_main_about() {
  local i=$1
  local in_param=''
  local fixed_arg_names=()
  local vararg_name=''
  local can_parse_options=1

  while [[ ${i} -lt $COMP_CWORD ]]; do
    if [[ ${can_parse_options} -eq 1 ]]; then
      case "${COMP_WORDS[$i]}" in
        --)
          can_parse_options=0
          (( i = i + 1 ));
          continue
          ;;
        -h|--help)
          __skip_opt_eq
          in_param=''
          continue
          ;;
      esac
    fi
    case "${COMP_WORDS[$i]}" in
      build-info)
        _main_about_build_info $(( i + 1 ))
        return
        ;;
      loggers)
        _main_about_loggers $(( i + 1 ))
        return
        ;;
      generate-completion)
        _main_about_generate_completion $(( i + 1 ))
        return
        ;;
      *)
        (( i = i + 1 ))
        # drop the head of the array
        fixed_arg_names=("${fixed_arg_names[@]:1}")
        ;;
    esac
  done
  local word="${COMP_WORDS[$COMP_CWORD]}"
  if [[ "${word}" =~ ^[-] ]]; then
    COMPREPLY=($(compgen -W '-h --help' -- "${word}"))
    return
  fi

  # We're either at an option's value, or the first remaining fixed size
  # arg, or the vararg if there are no fixed args left
  [[ -z "${in_param}" ]] && in_param=${fixed_arg_names[0]}
  [[ -z "${in_param}" ]] && in_param=${vararg_name}

  case "${in_param}" in
    --help)
      ;;
    *)
      COMPREPLY=($(compgen -W 'build-info loggers generate-completion' -- "${word}"))
      ;;
  esac
}

_main_about_build_info() {
  local i=$1
  local in_param=''
  local fixed_arg_names=()
  local vararg_name=''
  local can_parse_options=1

  while [[ ${i} -lt $COMP_CWORD ]]; do
    if [[ ${can_parse_options} -eq 1 ]]; then
      case "${COMP_WORDS[$i]}" in
        --)
          can_parse_options=0
          (( i = i + 1 ));
          continue
          ;;
        -h|--help)
          __skip_opt_eq
          in_param=''
          continue
          ;;
      esac
    fi
    case "${COMP_WORDS[$i]}" in
      *)
        (( i = i + 1 ))
        # drop the head of the array
        fixed_arg_names=("${fixed_arg_names[@]:1}")
        ;;
    esac
  done
  local word="${COMP_WORDS[$COMP_CWORD]}"
  if [[ "${word}" =~ ^[-] ]]; then
    COMPREPLY=($(compgen -W '-h --help' -- "${word}"))
    return
  fi

  # We're either at an option's value, or the first remaining fixed size
  # arg, or the vararg if there are no fixed args left
  [[ -z "${in_param}" ]] && in_param=${fixed_arg_names[0]}
  [[ -z "${in_param}" ]] && in_param=${vararg_name}

  case "${in_param}" in
    --help)
      ;;
  esac
}

_main_about_loggers() {
  local i=$1
  local in_param=''
  local fixed_arg_names=()
  local vararg_name=''
  local can_parse_options=1

  while [[ ${i} -lt $COMP_CWORD ]]; do
    if [[ ${can_parse_options} -eq 1 ]]; then
      case "${COMP_WORDS[$i]}" in
        --)
          can_parse_options=0
          (( i = i + 1 ));
          continue
          ;;
        --test)
          __skip_opt_eq
          in_param=''
          continue
          ;;
        -h|--help)
          __skip_opt_eq
          in_param=''
          continue
          ;;
      esac
    fi
    case "${COMP_WORDS[$i]}" in
      *)
        (( i = i + 1 ))
        # drop the head of the array
        fixed_arg_names=("${fixed_arg_names[@]:1}")
        ;;
    esac
  done
  local word="${COMP_WORDS[$COMP_CWORD]}"
  if [[ "${word}" =~ ^[-] ]]; then
    COMPREPLY=($(compgen -W '--test -h --help' -- "${word}"))
    return
  fi

  # We're either at an option's value, or the first remaining fixed size
  # arg, or the vararg if there are no fixed args left
  [[ -z "${in_param}" ]] && in_param=${fixed_arg_names[0]}
  [[ -z "${in_param}" ]] && in_param=${vararg_name}

  case "${in_param}" in
    --test)
      ;;
    --help)
      ;;
  esac
}

_main_about_generate_completion() {
  local i=$1
  local in_param=''
  local fixed_arg_names=('shell')
  local vararg_name=''
  local can_parse_options=1

  while [[ ${i} -lt $COMP_CWORD ]]; do
    if [[ ${can_parse_options} -eq 1 ]]; then
      case "${COMP_WORDS[$i]}" in
        --)
          can_parse_options=0
          (( i = i + 1 ));
          continue
          ;;
        -h|--help)
          __skip_opt_eq
          in_param=''
          continue
          ;;
      esac
    fi
    case "${COMP_WORDS[$i]}" in
      *)
        (( i = i + 1 ))
        # drop the head of the array
        fixed_arg_names=("${fixed_arg_names[@]:1}")
        ;;
    esac
  done
  local word="${COMP_WORDS[$COMP_CWORD]}"
  if [[ "${word}" =~ ^[-] ]]; then
    COMPREPLY=($(compgen -W '-h --help' -- "${word}"))
    return
  fi

  # We're either at an option's value, or the first remaining fixed size
  # arg, or the vararg if there are no fixed args left
  [[ -z "${in_param}" ]] && in_param=${fixed_arg_names[0]}
  [[ -z "${in_param}" ]] && in_param=${vararg_name}

  case "${in_param}" in
    --help)
      ;;
    shell)
      COMPREPLY=($(compgen -W 'bash zsh fish' -- "${word}"))
      ;;
  esac
}

