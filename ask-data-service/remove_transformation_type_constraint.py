#!/usr/bin/env python3
"""
数据库迁移脚本：删除 transformation_type 字段的 CHECK 约束
运行此脚本可以删除约束，让 transformation_type 字段接受任何值
"""

from src.utils.schema_database import SchemaDatabase
from src.config.settings import Settings

def remove_transformation_type_constraint():
    """删除 transformation_type 字段的 CHECK 约束"""
    try:
        settings = Settings()
        db = SchemaDatabase(settings)
        conn = db._get_connection()
        cursor = conn.cursor()

        # 检查约束是否存在
        cursor.execute("""
            SELECT conname
            FROM pg_constraint 
            WHERE conrelid = 'ask_data_dataset_transformations'::regclass
            AND contype = 'c'
            AND conname = 'ask_data_dataset_transformations_transformation_type_check'
        """)
        
        constraint_exists = cursor.fetchone() is not None
        
        if constraint_exists:
            print("🔍 发现 transformation_type 约束，正在删除...")
            
            # 删除约束
            cursor.execute('''
                ALTER TABLE ask_data_dataset_transformations 
                DROP CONSTRAINT ask_data_dataset_transformations_transformation_type_check
            ''')
            
            conn.commit()
            print("✅ transformation_type 约束删除成功")
            
        else:
            print("ℹ️ transformation_type 约束不存在，无需删除")
        
        conn.close()
        
    except Exception as e:
        print(f"❌ 删除约束失败: {e}")
        raise

if __name__ == "__main__":
    print("=" * 60)
    print("删除 transformation_type 字段的 CHECK 约束")
    print("=" * 60)
    
    remove_transformation_type_constraint()
    
    print("\n" + "=" * 60)
    print("迁移完成！现在 transformation_type 字段可以接受任何值")
    print("=" * 60)