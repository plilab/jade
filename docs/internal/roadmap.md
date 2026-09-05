# Roadmap

This page collects possible future work. It is not a commitment or a
replacement for tracked issues.

## Possible Ideas

- Add instructions for creating a symlink for `./jade`.
- Add an overview of the documentation site.

## Possible Research Projects

Ideas for projects that people could work on as part of internships,
coursework, or research:

### Bytecode Surveys

- Survey exception tables in bytecode.
- Survey uses of `monitorenter` and `monitorexit`.
- Survey class structure.

### Testing Framework

- Implement a diff-based test harness.
- Add a virtual filesystem for tests.

## Existing Maintenance Items

- Track code scanning alerts in issues using task lists.
- Review repository settings and features.
- Add a GitHub Action to build and report warnings.
- Use the internal compiler to generate class files for tests.
- Rename packages away from `ucombinator` if appropriate.
- Require KDoc on single-line functions where appropriate.
- Add GitHub Packages support.
- Add license-report and dependency-update workflows.
- Use Javadoc for Java source tests.
- Add JavaParser tests.
- Add CodeQL.
- Search for and correct `TODO` typos.

## Open Questions

- Could JavaParser type inference help solve generic type reconstruction?
- Should `./jade` be a shell script, with a corresponding Windows launcher?
- Should GitHub Discussions and other repository features be disabled?
