# AGENTS.md

Instructions for AI coding agents. The source of truth is [CLAUDE.md](CLAUDE.md) (stack, architecture,
conventions, commands); the product plan is [docs/PLAN.md](docs/PLAN.md).

Checklist before each commit:

1. Review the diff: no duplicated logic, files < ~300 lines, functions < ~40 lines, reuse `ui/common` and `domain/calc`.
2. `./gradlew assembleDebug` passes with no new warnings.
3. Update `CLAUDE.md` (architecture/status) and `README.md` (features/usage) to match the change.
4. Small commits, one feature each.
