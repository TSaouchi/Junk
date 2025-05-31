import xml.etree.ElementTree as ET

class GenericXMLFlattener:
    def __init__(self, xml_data, prefix):
        self.xml_data = xml_data
        self.prefix = prefix
        self.result = {}
   
    def flatten(self):
        root = ET.fromstring(self.xml_data)
        self._flatten_element(root)
        return self.result

    def _flatten_element(self, element):
        children = list(element)

        if not children:
            key = f"{self.prefix}.{element.tag}"
            self.result[key] = element.text.strip() if element.text else ""
        else:
            child_tags = [child.tag for child in children]

            if len(children) > 1 and all(tag.startswith(child_tags[0][:-1]) for tag in child_tags):
                list_key = f"{self.prefix}.{element.tag}_list"
                self.result[list_key] = child_tags

                for i, child in enumerate(children):
                    for grandchild in list(child):
                        key = f"{self.prefix}.{grandchild.tag}_{i}"
                        self.result[key] = grandchild.text.strip() if grandchild.text else ""
            else:
                for child in children:
                    self._flatten_element(child)

test_xmls = [
    # Example 1 (your original)
    """
    <result>
      <persons>
        <person1>
          <name>John</name>
          <age>30</age>
        </person1>
        <person2>
          <name>Jane</name>
          <age>25</age>
        </person2>
      </persons>
      <status>active</status>
    </result>
    """,

    # Example 2
    """
    <data>
      <items>
        <item>
          <id>100</id>
          <value>42</value>
        </item>
        <item>
          <id>101</id>
          <value>43</value>
        </item>
      </items>
      <summary>OK</summary>
    </data>
    """,

    # Example 3
    """
    <response>
      <metrics>
        <cpu>80</cpu>
        <memory>65</memory>
      </metrics>
      <status>running</status>
    </response>
    """,

    # Example 4
    """
    <root>
      <users>
        <user1>
          <username>alice</username>
          <email>alice@example.com</email>
        </user1>
        <user2>
          <username>bob</username>
          <email>bob@example.com</email>
        </user2>
      </users>
      <orders>
        <order1>
          <id>555</id>
          <amount>100</amount>
        </order1>
        <order2>
          <id>556</id>
          <amount>200</amount>
        </order2>
      </orders>
    </root>
    """,

    # Example 5
    "<simple>Hello World</simple>"
]

for i, xml in enumerate(test_xmls, 1):
    print(f"--- Test case {i} ---")
    flattener = GenericXMLFlattener(xml, "result2")
    result = flattener.flatten()
    import pprint
    pprint.pprint(result)
    print()

