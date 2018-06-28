(set-option :fixedpoint.engine datalog)
(declare-sort stmt)

; po (s1, s2): program order inside of function from s1 to s2
(declare-rel po (stmt stmt))
(declare-rel path (stmt stmt))

;;;; rules ;;;; 

; variable declaration
(declare-var s1 stmt)
(declare-var s2 stmt)
(declare-var s3 stmt)

(rule (=> (po s1 s2) (path s1 s2)))

; transitive po 
(rule (=>
       (and
        (path s1 s2)
        (path s2 s3))
       (path s1 s3)))
