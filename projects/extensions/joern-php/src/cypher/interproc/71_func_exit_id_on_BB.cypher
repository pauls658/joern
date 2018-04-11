match (bb:BB),(exit:Artificial{type:"CFG_FUNC_EXIT"}) where bb.funcid = exit.funcid set bb.exit_id = ID(exit);
