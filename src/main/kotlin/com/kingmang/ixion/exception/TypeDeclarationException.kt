package com.kingmang.ixion.exception

class TypeDeclarationException : IxException(
    40,
    "Invalid type declaration: {0}.",
    "Check the type name, alias dependencies and type parameters."
)
