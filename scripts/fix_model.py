#!/usr/bin/env python3
"""
Fix mBART ONNX model: convert use_cache_branch from required input to initializer.
This allows Java ONNX Runtime to run the model without boolean tensor support.
"""
import onnx
from onnx import helper, TensorProto

def fix_model(input_path, output_path):
    print(f"Loading model from: {input_path}")
    model = onnx.load(input_path)

    # Find use_cache_branch input
    use_cache_input = None
    for inp in model.graph.input:
        if inp.name == "use_cache_branch":
            use_cache_input = inp
            print(f"Found use_cache_branch input: {inp}")
            break

    if use_cache_input is None:
        print("use_cache_branch input not found.")
        onnx.save(model, output_path)
        return

    # Remove from graph inputs (make it an initializer instead)
    model.graph.input.remove(use_cache_input)

    # Add initializer with default value True
    init = helper.make_tensor(
        name="use_cache_branch",
        data_type=TensorProto.BOOL,
        dims=[1],
        vals=[True]
    )
    model.graph.initializer.append(init)

    print(f"Converted use_cache_branch to initializer with default=True")
    print(f"Saving to: {output_path}")

    onnx.checker.check_model(model)
    onnx.save(model, output_path)
    print("Done!")

if __name__ == "__main__":
    import sys
    if len(sys.argv) < 3:
        print("Usage: python fix_model.py <input_model.onnx> <output_model.onnx>")
        sys.exit(1)
    fix_model(sys.argv[1], sys.argv[2])
