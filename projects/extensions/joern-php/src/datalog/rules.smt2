(set-option :fixedpoint.engine datalog)
(define-sort var() (_ BitVec 32))
(define-sort stmt() (_ BitVec 32))
(define-sort fid() (_ BitVec 32))
(define-sort pramNum() (_ BitVec 32))

; sen (s1): s1 is a sensitive assignment
(declare-rel sen (stmt))

; tainted (s1): s1 is a tainted statement
(declare-rel tainted (stmt))

; echo (s1): s1 is echo statement
(declare-rel echo (stmt))

; reachable (v1 s1 s2): v1 at s1 is reachable to echo statement s2
(declare-rel reachable (var stmt stmt))

; po (s1, s2): program order inside of function from s1 to s2 in function f1
(declare-rel po (stmt stmt))

; write (v1, s1): store instruction of v1 at s1 in function f1
(declare-rel write (var stmt))

; read (v1, s1): load instruction of v1 at s1 in function f1
(declare-rel read (var stmt))

; assign (v1 v2 s1): v1 = v2 at s1
(declare-rel assign (var var stmt))

; alive (v1, s1, s2): store value of v1 at s1 is still alive at s2
(declare-rel alive (var stmt stmt))

; dataDep (v1 s1 v2 s2): data dependency from v1 at s1 to v2 at s2
(declare-rel dataDep (var stmt var stmt))

;;;; rules ;;;; 

; variable declaration
(declare-var s1 stmt)
(declare-var s2 stmt)
(declare-var s3 stmt)

(declare-var f1 fid)
(declare-var f2 fid)

(declare-var v1 var)
(declare-var v2 var)
(declare-var v3 var)
(declare-var v4 var)


;; liveness is created only for sensitive statement
(rule (=>
       (and
        (sen s1)
        (write v1 s1))
       (alive v1 s1 s1)))

;; create assign relation (it is not necessary but for understanding better)
(rule (=>
       (and
        (write v1 s1)
        (read v2 s1))
       (assign v1 v2 s1)))


;; liveness propagation
;; at each statement, which variable defs may still be alive?
(rule (=> 
       (and 
        (po s1 s2)
        (alive v1 s3 s1)
        (or 
         (= s1 s3)
         (not (write v1 s1))))
       (alive v1 s3 s2)))

;; getting tainted information
(rule (=>
       (and
        (alive v1 s1 s2)
        (read v1 s2))
       (tainted s2)))

;; create liveness info based on tainted info
(rule (=>
       (and
        (tainted s1)
        (write v1 s1))
       (alive v1 s1 s1)))

;; checking reachability to echo
(rule (=>
       (and
        (alive v1 s1 s2)
        (read v1 s2)
        (echo s2))
       (reachable v1 s1 s2)))

;; dataDep for tainted info
(rule (=>
       (and
        (alive v1 s1 s2)
        (assign v2 v1 s2))
       (dataDep v1 s1 v2 s2)))

;; transitive closure for dataDep and reachability
(rule (=>
       (and
        (dataDep v1 s1 v2 s2)
        (dataDep v2 s2 v3 s3))
       (dataDep v1 s1 v3 s3)))

(rule (=>
       (and
        (dataDep v1 s1 v2 s2)
        (reachable v2 s2 s3))
       (reachable v1 s1 s3)))

