# Contributing

Thanks for your interest in contributing to Adaptive Theme.

This project is intentionally maintained with a narrow scope and a strong preference
for consistency, simplicity, and long-term maintainability over rapid expansion.
Please read these guidelines before proposing or submitting changes.

## Issues First

Please open an issue before doing implementation work.

Pull requests are accepted only when:

- they address an existing issue;
- the issue has been discussed and accepted by the maintainer; and
- the maintainer has explicitly invited or approved implementation.

An open issue does not automatically mean that a change is wanted. Please wait for
clear approval before starting work.

Unsolicited pull requests may be closed without detailed review, even when the code
is functional.

## Project Direction

Adaptive Theme is intentionally straightforward, focused, and easy to understand.
Changes must preserve a clean and uncluttered interface. New functionality is not
automatically an improvement, especially when it adds screens, controls, settings,
navigation, dependencies, or concepts that users must learn.

Feature and UI proposals should explain:

- why the feature belongs in Adaptive Theme;
- why the existing interface cannot accommodate the use case;
- what complexity the change introduces;
- whether a simpler solution was considered; and
- how the interface remains clear for users who do not need the feature.

The project generally prefers:

- fewer, clearer controls;
- sensible defaults over additional settings;
- established interaction patterns over novel ones;
- focused screens over dense collections of options;
- removing complexity over documenting it; and
- maintaining the current scope over supporting every possible workflow.

Changes may be declined solely because they make the app more complicated, visually
busy, or harder to maintain.

Do not substantially alter the visual language, navigation, information hierarchy,
or interaction model without prior discussion and explicit approval.

## Before Opening an Issue

Please:

- search existing and closed issues for prior discussion;
- confirm that the problem occurs on the latest version;
- provide clear reproduction steps for bugs;
- explain the use case rather than prescribing only a solution; and
- keep each issue focused on one problem or proposal.

For feature requests, describe:

- the problem being solved;
- why it belongs in this project;
- possible alternatives;
- the expected behavior.

Issues that are vague, speculative, duplicated, or outside the intended scope may
be closed.

## Pull Requests

After an issue has been approved for implementation:

- keep the change limited to the agreed scope;
- reference the issue in the pull request;
- avoid unrelated cleanup or refactoring;
- follow the existing architecture and coding style;
- add or update tests where applicable;
- update documentation when behavior changes; and
- ensure all checks pass.

Do not introduce new dependencies, change public APIs, reformat unrelated files, or
redesign surrounding code without prior approval.

Large pull requests may be rejected in favor of smaller, independently reviewable
changes. Pull requests must target branch "develop".

## Commit Messages

Every commit and pull-request title must follow
[Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).

The required format is:

```text
<type>[optional scope]: <description> (#<optional issue number>)
```

Commit descriptions must be concise, written in the imperative mood, and describe
the actual change. Do not use vague messages such as `update code`, `fix stuff`, or
`changes`.

Contributors may be asked to reword, squash, or reorganize commits before a pull
request is merged.

## Quality Expectations

Contributions must be complete and production-ready. Draft implementations,
placeholder behavior, unexplained generated code, and "fix later" follow-ups are
generally not accepted.

If AI-assisted tools were materially used, disclose that in the pull request and
confirm that you reviewed, understood, and tested the resulting code.

## Review

Review is not guaranteed and may take time. The maintainer may request changes based
on design consistency, long-term maintenance cost, or project direction, even when
the implementation is technically correct.

A contribution may be declined or closed when it:

- increases complexity without sufficient benefit;
- conflicts with the project's direction;
- creates an ongoing maintenance burden;
- expands the agreed scope;
- lacks adequate testing or documentation; or
- does not follow these guidelines.

The maintainer retains final discretion over what is included in the project.

## Small Corrections

For trivial documentation or small fixes, opening an issue is preferred. If you are unsure whether a change is trivial, open an issue.

## Conduct

Be respectful, concise, and constructive. Disagreement is welcome; hostility,
pressure, and repeated attempts to reopen settled decisions are not.
