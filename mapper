def transform_request_stat(request_stat):
    # Result dictionary structure
    transformed_dict = {
        "pomp_id_node_name": {},
        "request_node_name": {},
        "result_node_name": {}
    }

    # Variables to track prefixes
    pomp_id_prefix = None
    result_prefix = None

    # Single pass: Process all keys and populate results dynamically
    for key, value in request_stat.items():
        # Detect keys ending with `.pomp_id` and set the prefix
        if key.endswith(".pomp_id"):
            pomp_id_prefix = value
            transformed_dict["request_node_name"]["pomp_id"] = "pomp_id_node_name"
        
        # Detect keys ending with `.result` and set the result prefix
        elif key.endswith(".result"):
            result_prefix = value
            transformed_dict["request_node_name"]["result"] = value
        
        # Process all `request_name.*` keys (those starting with `request_name.`)
        elif key.startswith("request_name."):
            transformed_dict["request_node_name"][key.split(".", 1)[1]] = value
        
        # Handle keys related to the `pomp_id_prefix`
        elif pomp_id_prefix and key.startswith(f"{pomp_id_prefix}."):
            transformed_dict["pomp_id_node_name"][key.split(".", 1)[1]] = value
        
        # Handle keys related to the `result_prefix`
        elif result_prefix and key.startswith(f"{result_prefix}."):
            # Replace `pomp_id_prefix` in the value if necessary
            if isinstance(value, list):
                value = ["pomp_id_node_name" if v == pomp_id_prefix else v for v in value]
            elif value == pomp_id_prefix:
                value = "pomp_id_node_name"
            transformed_dict["result_node_name"][key.split(".", 1)[1]] = value

    return transformed_dict
