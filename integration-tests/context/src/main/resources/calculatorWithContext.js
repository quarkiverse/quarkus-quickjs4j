function add(a, b) {
    return a + b;
}

function multiply(a, b) {
    return Calculator_Builtins.javaMultiply(a, b);
}

function divide(a, b) {
    if (b === 0) {
        throw new Error("Division by zero");
    }
    return a / b;
}
export {
  add, multiply, divide
};
