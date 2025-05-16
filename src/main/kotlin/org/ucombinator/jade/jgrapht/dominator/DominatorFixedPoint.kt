// Reference implementation based on fixed-point equation
// Dom(v) = { v } + Union for u in pred(v) of Dom(u)
//   Do greatest-fixed point starting from Dom(root) = { root }, Dom(v) = all vertexes
// Idom(v) = u in Dom(v) such that u not in Dom(Dom(v))
