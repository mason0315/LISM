import pandas as pd
from neo4j import GraphDatabase
import openpyxl
import os
import sys
import socket

# Neo4j数据库连接配置
NEO4J_URI = "neo4j://127.0.0.1:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "12345678"


def check_database_connection(host="127.0.0.1", port=7687):
    """检查数据库连接"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        result = sock.connect_ex((host, port))
        sock.close()
        if result == 0:
            print(f"数据库端口 {host}:{port} 可访问")
            return True
        else:
            print(f"数据库端口 {host}:{port} 不可访问，错误代码: {result}")
            return False
    except Exception as e:
        print(f"检查数据库连接时出错: {e}")
        return False

class KnowledgeGraphBuilder:
    def __init__(self, uri, user, password):
        try:
            self.driver = GraphDatabase.driver(uri, auth=(user, password))
        except Exception as e:
            print(f"连接数据库失败: {e}")
            sys.exit(1)
    
    def close(self):
        if self.driver:
            self.driver.close()
    
    def clear_database(self):
        """清空数据库中的所有数据"""
        try:
            with self.driver.session() as session:
                session.run("MATCH (n) DETACH DELETE n")
        except Exception as e:
            print(f"清空数据库时出错: {e}")
            raise
    
    def create_constraints(self):
        """创建唯一性约束"""
        try:
            with self.driver.session() as session:
                # 为各类节点创建唯一性约束
                session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (b:Book) REQUIRE b.id IS UNIQUE")
                session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (a:Author) REQUIRE a.id IS UNIQUE")
                session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Publisher) REQUIRE p.id IS UNIQUE")
                session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (c:Category) REQUIRE c.id IS UNIQUE")
        except Exception as e:
            print(f"创建约束时出错: {e}")
            raise
    
    def add_book(self, book_id, title, isbn=None, publish_year=None):
        """添加图书节点"""
        with self.driver.session() as session:
            try:
                session.run("""
                    MERGE (b:Book {id: $book_id})
                    SET b.title = $title, b.isbn = $isbn, b.publish_year = $publish_year
                """, book_id=book_id, title=title, isbn=isbn, publish_year=publish_year)
            except Exception as e:
                print(f"插入图书数据时出错: {e}")
                raise
    
    def add_author(self, author_id, name, nationality=None):
        """添加作者节点"""
        with self.driver.session() as session:
            try:
                session.run("""
                    MERGE (a:Author {id: $author_id})
                    SET a.name = $name, a.nationality = $nationality
                """, author_id=author_id, name=name, nationality=nationality)
            except Exception as e:
                print(f"插入作者数据时出错: {e}")
                raise
    
    def add_publisher(self, publisher_id, name, country=None):
        """添加出版社节点"""
        with self.driver.session() as session:
            try:
                session.run("""
                    MERGE (p:Publisher {id: $publisher_id})
                    SET p.name = $name, p.country = $country
                """, publisher_id=publisher_id, name=name, country=country)
            except Exception as e:
                print(f"插入出版社数据时出错: {e}")
                raise
    
    def add_category(self, category_id, name):
        """添加分类节点"""
        with self.driver.session() as session:
            try:
                session.run("""
                    MERGE (c:Category {id: $category_id})
                    SET c.name = $name
                """, category_id=category_id, name=name)
            except Exception as e:
                print(f"插入分类数据时出错: {e}")
                raise
    
    def add_author_book_relation(self, author_id, book_id):
        """添加作者-图书关系"""
        try:
            with self.driver.session() as session:
                session.run("""
                    MATCH (a:Author {id: $author_id})
                    MATCH (b:Book {id: $book_id})
                    MERGE (a)-[:WRITES]->(b)
                """, author_id=author_id, book_id=book_id)
        except Exception as e:
            print(f"添加作者-图书关系时出错: {e}")
            raise
    
    def add_publisher_book_relation(self, publisher_id, book_id):
        """添加出版社-图书关系"""
        try:
            with self.driver.session() as session:
                session.run("""
                    MATCH (p:Publisher {id: $publisher_id})
                    MATCH (b:Book {id: $book_id})
                    MERGE (p)-[:PUBLISHES]->(b)
                """, publisher_id=publisher_id, book_id=book_id)
        except Exception as e:
            print(f"添加出版社-图书关系时出错: {e}")
            raise
    
    def add_book_category_relation(self, book_id, category_id):
        """添加图书-分类关系"""
        try:
            with self.driver.session() as session:
                session.run("""
                    MATCH (b:Book {id: $book_id})
                    MATCH (c:Category {id: $category_id})
                    MERGE (b)-[:BELONGS_TO]->(c)
                """, book_id=book_id, category_id=category_id)
        except Exception as e:
            print(f"添加图书-分类关系时出错: {e}")
            raise

def read_excel_files():
    """读取Excel文件"""
    # 读取各Excel文件
    try:
        books_df = pd.read_excel('book.xlsx')
        authors_df = pd.read_excel('作者.xlsx')
        publishers_df = pd.read_excel('出版社.xlsx')
        categories_df = pd.read_excel('分类.xlsx')
        relations_df = pd.read_excel('关系.xlsx')
        
        return books_df, authors_df, publishers_df, categories_df, relations_df
    except Exception as e:
        print(f"读取Excel文件时出错: {e}")
        return None, None, None, None, None

def build_knowledge_graph():
    """构建知识图谱并导入Neo4j"""
    # 读取数据
    books_df, authors_df, publishers_df, categories_df, relations_df = read_excel_files()
    
    if books_df is None:
        print("无法读取数据文件，请检查文件是否存在且格式正确。")
        return
    
    # 检查数据库连接
    print("检查数据库连接...")
    if not check_database_connection():
        print("请确保Neo4j数据库正在运行，并且连接参数正确。")
        return
    
    # 连接数据库
    print("连接数据库...")
    kg_builder = KnowledgeGraphBuilder(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)
    
    try:
        # 清空数据库
        print("清空数据库...")
        kg_builder.clear_database()
        
        # 创建约束
        print("创建约束...")
        kg_builder.create_constraints()
        
        # 插入作者数据
        print("插入作者数据...")
        for _, row in authors_df.iterrows():
            # 实际列名为author_id, name, nationality
            author_id = row.get('author_id', '')
            name = row.get('name', '')
            nationality = row.get('nationality', '')
            if pd.notna(author_id) and pd.notna(name):
                # 处理国籍字段
                processed_nationality = str(nationality) if pd.notna(nationality) and nationality != '' else None
                kg_builder.add_author(str(author_id), str(name), processed_nationality)
        
        # 插入出版社数据
        print("插入出版社数据...")
        for _, row in publishers_df.iterrows():
            # 实际列名为publisher_id, name
            publisher_id = row.get('publisher_id', '')
            name = row.get('name', '')
            # 出版社表中没有country字段
            if pd.notna(publisher_id) and pd.notna(name):
                kg_builder.add_publisher(str(publisher_id), str(name), None)
        
        # 插入分类数据
        print("插入分类数据...")
        for _, row in categories_df.iterrows():
            # 实际列名为category_id, name
            category_id = row.get('category_id', '')
            name = row.get('name', '')
            if pd.notna(category_id) and pd.notna(name):
                kg_builder.add_category(str(category_id), str(name))
        
        # 插入图书数据
        print("插入图书数据...")
        for _, row in books_df.iterrows():
            # 实际列名为isbn, title, publish_date
            book_id = row.get('isbn', '')  # 使用isbn作为图书ID
            title = row.get('title', '')
            isbn = row.get('isbn', '')
            publish_date = row.get('publish_date', '')
            
            if pd.notna(book_id) and pd.notna(title):
                # 处理出版年份，从publish_date中提取年份
                processed_publish_year = None
                if pd.notna(publish_date) and publish_date != '':
                    try:
                        # 假设publish_date是日期格式，提取年份
                        if isinstance(publish_date, str):
                            processed_publish_year = int(publish_date.split('-')[0])
                        else:
                            processed_publish_year = publish_date.year
                    except Exception as e:
                        print(f"警告: 图书'{title}'的出版日期'{publish_date}'解析失败: {e}")
                
                kg_builder.add_book(str(book_id), str(title), 
                                   str(isbn) if pd.notna(isbn) else None,
                                   processed_publish_year)
        
        # 插入关系数据
        print("插入关系数据...")
        for _, row in relations_df.iterrows():
            # 实际列名为start_node_id, relationship_type, end_node_id
            start_node_id = row.get('start_node_id', '')
            relationship_type = row.get('relationship_type', '')
            end_node_id = row.get('end_node_id', '')
            
            if pd.notna(relationship_type) and pd.notna(start_node_id) and pd.notna(end_node_id):
                # 根据关系类型调用相应的方法
                if relationship_type == 'CREATED':  # 作者创作图书
                    kg_builder.add_author_book_relation(str(start_node_id), str(end_node_id))
                elif relationship_type == 'PUBLISHED':  # 出版社出版图书
                    kg_builder.add_publisher_book_relation(str(start_node_id), str(end_node_id))
                elif relationship_type == 'BELONGS_TO':  # 图书属于分类
                    kg_builder.add_book_category_relation(str(start_node_id), str(end_node_id))
                else:
                    print(f"未知的关系类型: {relationship_type}")
        
        print("知识图谱构建完成！")
        
    except Exception as e:
        print(f"构建知识图谱时出错: {e}")
        import traceback
        traceback.print_exc()
    finally:
        kg_builder.close()

if __name__ == "__main__":
    build_knowledge_graph()