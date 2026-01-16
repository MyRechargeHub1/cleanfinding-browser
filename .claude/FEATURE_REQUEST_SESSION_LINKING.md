# Feature Request: Session Linking - Reference Previous Conversation Context

## Problem Statement

Currently, each Claude Code session is completely independent. When working on a project across multiple sessions, there's no way to maintain continuity or context from previous conversations. This means:

- Users must re-explain context in each new session
- Previous decisions and discussions are lost
- Multi-session projects require manual documentation
- Inefficient use of time repeating information

## Proposed Solution

Add a `--link-session` (or similar) CLI flag that allows users to explicitly link a new session to one or more previous sessions.

### Suggested Implementation

```bash
# Link to a single previous session
claude-code --link-session=WJT8y

# Link to multiple previous sessions
claude-code --link-sessions=WJT8y,TblxK

# Or perhaps a more explicit syntax
claude-code --with-context-from=claude/document-sessions-WJT8y
```

When linked, Claude would have access to:
- Previous conversation history (or summary)
- Context about decisions made
- Outstanding tasks or issues discussed
- Relevant session documentation

## Benefits

1. **Continuity**: Seamless context across multi-session work
2. **Efficiency**: No need to re-explain background information
3. **User Control**: Explicit linking gives users privacy and relevance control
4. **Better Outcomes**: More informed decisions based on historical context
5. **Complex Projects**: Makes long-term repository work more practical

## Use Cases

1. **Multi-day projects**: Pick up where you left off yesterday
2. **Related work**: Link to a previous session that did similar work
3. **Follow-ups**: "Remember that bug we discussed? Here's the fix"
4. **Knowledge building**: Each session builds on previous learnings

## Alternative: Automatic Linking

An alternative would be automatic linking of all sessions in a repository, but this has drawbacks:
- May load irrelevant context
- Privacy concerns with sensitive discussions
- Could impact performance/cost

**Manual linking provides the best balance of power and control.**

## Implementation Notes

- Session IDs are already tracked (e.g., `WJT8y`, `TblxK`)
- Git branches already follow pattern `claude/<description>-<session-id>`
- Could store conversation summaries in `.claude/sessions/` directory
- Link could reference either session ID or branch name

## Priority

**Medium-High** - This would significantly improve the multi-session user experience for complex projects.

## Related Issues

None identified yet.

---

**Session Context**: This request emerged from session `WJT8y` where the lack of continuity from previous session `TblxK` was noted.
