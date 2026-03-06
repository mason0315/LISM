from neo4j import GraphDatabase

# Neo4j数据库连接配置
NEO4J_URI = "neo4j://127.0.0.1:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "12345678"

def verify_graph():
    """验证知识图谱导入结果"""
    try:
        driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))
        with driver.session() as session:
            # 统计各类节点数量
            result = session.run("MATCH (n) RETURN count(n) AS total")
            record = result.single()
            print(f"节点总数: {record['total']}")
            
            result = session.run("MATCH (n:Book) RETURN count(n) AS count")
            record = result.single()
            print(f"图书节点数: {record['count']}")
            
            result = session.run("MATCH (n:Author) RETURN count(n) AS count")
            record = result.single()
            print(f"作者节点数: {record['count']}")
            
            result = session.run("MATCH (n:Publisher) RETURN count(n) AS count")
            record = result.single()
            print(f"出版社节点数: {record['count']}")
            
            result = session.run("MATCH (n:Category) RETURN count(n) AS count")
            record = result.single()
            print(f"分类节点数: {record['count']}")
            
            # 统计关系数量
            result = session.run("MATCH ()-[r]->() RETURN count(r) AS total")
            record = result.single()
            print(f"关系总数: {record['total']}")
            
            result = session.run("MATCH ()-[r:WRITES]->() RETURN count(r) AS count")
            record = result.single()
            print(f"作者->图书关系数: {record['count']}")
            
            result = session.run("MATCH ()-[r:PUBLISHES]->() RETURN count(r) AS count")
            record = result.single()
            print(f"出版社->图书关系数: {record['count']}")
            
            result = session.run("MATCH ()-[r:BELONGS_TO]->() RETURN count(r) AS count")
            record = result.single()
            print(f"图书->分类关系数: {record['count']}")
            
            # 显示一些示例数据
            print("\n示例图书数据:")
            result = session.run("MATCH (b:Book) RETURN b.title AS title, b.isbn AS isbn LIMIT 5")
            for record in result:
                print(f"  - {record['title']} (ISBN: {record['isbn']})")
            
            print("\n示例作者数据:")
            result = session.run("MATCH (a:Author) RETURN a.name AS name LIMIT 5")
            for record in result:
                print(f"  - {record['name']}")
            
        driver.close()
        print("\n验证完成!")
        
    except Exception as e:
        print(f"验证过程中出错: {e}")

if __name__ == "__main__":
    verify_graph()