## Algorithms

### Ordering

Step 1: Find Nesting Structure

- Use Dominators to find loops
- What remains is a DAG within each loop
- Resulting code:

  ```text
  ...
  L1: while (true) {
    ...
    if (...) { break L1; }
    ...
  }
  ...
  ```

Step 2: Order the DAGS

- Some orderings respect internal edges and thus avoid an extra "break" (i.e., GOTO).
- Each vertex has at most one preferred next vertex, when that is not the next edge, we pay in an extra "break"/GOTO.
- Each vertex may be the preferred next vertext of multiple vertexes.
- Due to the DAG constraint, we can satisfy the preferred next vetex only when there are no other vertexes on the path between
  the two vertexes.  Since those can never be satisfied, we consider only the cases when there are no other vertexes on paths between them.
- When a vertex is the preferred next vertex of multiple vertexes, we can satisfy at most one and since we only consider cases
  when there are no vertexes on paths between them, all of the others are immediate predicesors.  Thus the cost of satisfying
  any one is the same as for any other.
- Thus the greedy algorithm that always picks a viable preferred next vertex when it is available is optimal.
- To ensure stability, we choose a tie breaker of choosing the earliest bytecode offset first.

Step 3: Remove labels that are not used.

### Dead-store elimination

`Elimination` removes local-variable assignments and declarations whose values are not live after the statement.
It runs after constant propagation in three phases:

1. Build a statement-level control-flow graph.
2. Compute `liveIn` and `liveOut` sets.
3. Clone the input and remove stores whose targets are not live.

Each invocation has independent graph and analysis state. Empty blocks have no entry or nodes.

The analysis currently tracks syntactic names and supports sequential statements, nested blocks, while loops, and labels.
It conservatively retains expressions that may have side effects or throw, but does not yet model every Java control-flow construct.
