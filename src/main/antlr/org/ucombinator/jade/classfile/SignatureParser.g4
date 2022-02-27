parser grammar SignatureParser;

@header {
package org.ucombinator.jade.classfile;
}

options {
  tokenVocab = Character;
}

// Up to date for: JVMS 17

identifier: ~( '.' | ';' | '[' | '/' | '<' | '>' | ':' )+ ;

baseType
: 'B' # Byte
| 'C' # Char
| 'D' # Double
| 'F' # Float
| 'I' # Int
| 'J' # Long
| 'S' # Short
| 'Z' # Boolean
;

voidDescriptor
: 'V' # Void
;

////////////////////////////////////////////////////////////////
// Java type signature

javaTypeSignature
: referenceTypeSignature # JavaTypeReference
| baseType # JavaTypeBase
;

////////////////////////////////////////////////////////////////
// Reference type signature

referenceTypeSignature
: classTypeSignature # ReferenceTypeClass
| typeVariableSignature # ReferenceTypeVariable
| arrayTypeSignature # ReferenceTypeArray
;

classTypeSignature
: 'L' packageSpecifier? simpleClassTypeSignature classTypeSignatureSuffix* ';'
;

packageSpecifier
: identifier '/' packageSpecifier*
;

simpleClassTypeSignature
: identifier typeArguments?
;

typeArguments
: '<' typeArgument typeArgument* '>'
;

typeArgument
: wildcardIndicator? referenceTypeSignature # TypeArgumentNonStar
| '*' # TypeArgumentStar
;

wildcardIndicator
: '+' # WildcardPlus
| '-' # WildcardMinus
;

classTypeSignatureSuffix
: '.' simpleClassTypeSignature
;

typeVariableSignature
: 'T' identifier ';'
;

arrayTypeSignature
: '[' javaTypeSignature
;

////////////////////////////////////////////////////////////////
// Class signature

classSignature
: typeParameters? superclassSignature superinterfaceSignature*
;

typeParameters
: '<' typeParameter typeParameter* '>'
;

typeParameter
: identifier classBound interfaceBound*
;

classBound
: ':' referenceTypeSignature?
;

interfaceBound
: ':' referenceTypeSignature
;

superclassSignature
: classTypeSignature
;

superinterfaceSignature
: classTypeSignature
;

////////////////////////////////////////////////////////////////
// Method signature

methodSignature
: typeParameters? '(' javaTypeSignature* ')' result throwsSignature*
;

result
: javaTypeSignature # ResultNonVoid
| voidDescriptor # ResultVoid
;

throwsSignature
: '^' classTypeSignature # ThrowsClass
| '^' typeVariableSignature # ThrowsVariable
;

////////////////////////////////////////////////////////////////
// Field signature

fieldSignature
: referenceTypeSignature
;
