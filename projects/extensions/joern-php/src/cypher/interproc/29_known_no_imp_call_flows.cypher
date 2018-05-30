// childnums match or * -> ret
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
exists(call.name) and
call.name in ["escapeshellarg", "utf8_encode", "utf8_decode", "stripslashes", "chr", "ord", "quotemeta", "array_unique", "join", "implode", "shell_exec", "ngettext", "mktime", "basename", "hexdec", "pack", "sprintf", "array_diff"] and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and aentry.childnum = aexit.childnum)
 or aexit.type = "return")
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;

// childnums match or 0,2 -> ret
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
exists(call.name) and
call.name in ["strtr", "array_pad"] and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and aentry.childnum = aexit.childnum) or 
 (aexit.type = "return" and (aentry.childnum = 0 or aentry.childnum = 2)))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;

// childnums match or 0,3 -> ret
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
exists(call.name) and
call.name in ["array_splice"] and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and aentry.childnum = aexit.childnum) or 
 (aexit.type = "return" and (aentry.childnum = 0 or aentry.childnum = 3)))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;

// childnums match or 0,1 -> ret
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
exists(call.name) and
call.name in ["substr_replace"] and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and aentry.childnum = aexit.childnum) or 
 (aexit.type = "return" and (aentry.childnum = 0 or aentry.childnum = 1)))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;



// childnums match or 1,2 -> ret
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
exists(call.name) and
call.name in ["str_replace", "preg_replace"] and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and aentry.childnum = aexit.childnum) or 
 (aexit.type = "return" and (aentry.childnum = 1 or aentry.childnum = 2)))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;

// childnums match or 2 -> ret
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
exists(call.name) and
call.name in ["preg_replace_callback"] and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and aentry.childnum = aexit.childnum) or 
 (aexit.type = "return" and aentry.childnum = 2))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;

// childnums match or 1 -> ret
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
exists(call.name) and
call.name in ["mhash", "array_search", "preg_split", "array_map", "explode"] and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and aentry.childnum = aexit.childnum) or 
 (aexit.type = "return" and aentry.childnum = 1))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;


// childnums match or 0 -> ret
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
exists(call.name) and
call.name in ["strrchr", "end", "escapeshellcmd", "dechex", "strrev", "chunk_split", "uniqid", "mb_convert_kana", "mb_strimwidth", "mb_decode_mimeheader", "mb_strcut", "decbin", "ceil", "floor", "round", "strtotime", "array_reverse", "parse_url", "base_convert", "ucfirst", "getenv", "htmlspecialchars", "mb_substr", "print_r", "base64_decode", "base64_encode", "abs", "intval", "realpath", "htmlentities", "array_filter", "md5", "gethostbyname", "strchr", "strstr", "arsort", "asort", "ksort", "addslashes", "addcslashes", "preg_quote", "str_repeat", "stristr", "nl2br", "array_slice", "array_values", "array_keys", "mb_convert_encoding", "substr", "chop", "rtrim", "trim", "strip_tags", "unserialize"] and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and aentry.childnum = aexit.childnum) or 
 (aexit.type = "return" and aentry.childnum = 0))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;

// childnums match or 1 -> 2
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST{type:"arg_exit"})
where 
exists(call.name) and
call.name in ["eregi", "ereg", "preg_match", "preg_match_all"] and
not (call)-[:CALLS]->() and
(aentry.childnum = aexit.childnum or 
 (aentry.childnum = 1 and aexit.childnum = 2))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;


// childnums match or 1 -> 0
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST{type:"arg_exit"})
where 
exists(call.name) and
call.name in ["array_unshift", "define"] and
not (call)-[:CALLS]->() and
(aentry.childnum = aexit.childnum or
 (aentry.childnum = 1 and aexit.childnum = 0))
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;

// childnums match
match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST{type:"arg_exit"})
where 
exists(call.name) and
call.name in ["popen", "pclose", "strftime", "similar_text", "assert_options", "assert", "bind_textdomain_codeset", "setlocale", "mb_language", "mb_http_output", "umask", "usleep", "rewind", "is_bool", "count_chars", "get_class", "is_resource", "range", "create_function", "trigger_error", "strcspn", "gmdate", "mkdir", "session_set_cookie_params", "copy", "ini_set", "mb_internal_encoding", "mb_strlen", "mb_strpos", "mt_srand", "crc32", "strcasecmp", "strnatcasecmp", "ldap_search", "ldap_count_entries", "ldap_get_entries", "ldap_free_result", "ldap_connect", "ldap_error", "ldap_set_option", "ldap_bind", "is_string", "mt_rand", "is_null", "get_html_translation_table", "array_key_exists", "dir", "fileowner", "is_array", "array_walk", "func_get_arg", "fread", "chmod", "mcrypt_generic_deinit", "mcrypt_module_close", "mcrypt_module_open", "mcrypt_create_iv", "mcrypt_enc_get_iv_size", "mcrypt_generic_init", "mcrypt_generic", "mdecrypt_generic", "error_log", "socket_set_blocking", "socket_set_block", "settype", "gettype", "set_time_limit", "unlink", "exec", "is_int", "fgetcsv", "flock", "printf", "feof", "opendir", "readdir", "closedir", "putenv", "strcmp", "class_exists", "fgets", "fputs","fsockopen", "extension_loaded", "is_executable", "is_file", "is_dir", "is_writable", "is_readable", "posix_getpwuid", "posix_getgrgid", "date", "filemtime", "file", "substr_count", "session_module_name", "session_set_save_handler", "is_numeric", "rename", "move_uploaded_file", "is_uploaded_file", "filesize", "fwrite", "fopen", "fclose", "file_exists", "header", "sizeof", "count", "array_push", "in_array", "strlen", "strpos", "strrpos", "function_exists", "mb_detect_encoding"] and
not (call)-[:CALLS]->() and
aentry.childnum = aexit.childnum
create 
(aentry)-[:REACHES]->(aexit)
return distinct ID(call) as donecallid;
