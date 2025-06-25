#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成全国行政区代码SQL插入脚本
解析 xzqdm.txt 文件，生成完整的 SQL INSERT 语句
"""

import uuid

def generate_uuid():
    """生成UUID字符串"""
    return str(uuid.uuid4())

def parse_xzqdm_file(filename):
    """解析行政区代码文件"""
    provinces = {}  # 省份数据
    cities = {}     # 地市数据  
    counties = []   # 县区数据
    
    # 直辖市代码
    direct_cities = ['110000', '120000', '310000', '500000']
    
    with open(filename, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
                
            parts = line.split('\t')
            if len(parts) < 2:
                continue
                
            code = parts[0].strip()
            name = parts[1].strip()
            
            # 判断行政区级别
            if code.endswith('0000'):  # 省级
                provinces[code] = name
            elif code.endswith('00'):  # 地市级
                province_code = code[:2] + '0000'
                if province_code not in cities:
                    cities[province_code] = []
                cities[province_code].append((code, name))
            else:  # 县区级
                province_code = code[:2] + '0000'
                if province_code in direct_cities:
                    # 直辖市：区级直接属于省级
                    city_code = province_code
                else:
                    # 其他省份：区县属于地市级
                    city_code = code[:4] + '00'
                counties.append((code, name, city_code, province_code))
    
    return provinces, cities, counties

def get_dic_id_mapping():
    """获取省份到dic_id的映射"""
    mapping = {
        '110000': '1935612311051014146',  # 北京
        '120000': '1935612311051014147',  # 天津
        '130000': '1935612311051014148',  # 河北
        '140000': '1935612311051014149',  # 山西
        '150000': '1935612311051014150',  # 内蒙古
        '210000': '1935612311051014151',  # 辽宁
        '220000': '1935612311051014152',  # 吉林
        '230000': '1935612311051014153',  # 黑龙江
        '310000': '1935612311051014154',  # 上海
        '320000': '1935612311051014155',  # 江苏
        '330000': '1935612311051014156',  # 浙江
        '340000': '1935612311051014157',  # 安徽
        '350000': '1935612311051014158',  # 福建
        '360000': '1935612311051014159',  # 江西
        '370000': '1935612311051014160',  # 山东
        '410000': '1935612311051014161',  # 河南
        '420000': '1935612311051014162',  # 湖北
        '430000': '1935612311051014163',  # 湖南
        '440000': '1935612311051014164',  # 广东
        '450000': '1935612311051014165',  # 广西
        '460000': '1935612311051014166',  # 海南
        '500000': '1935612311051014167',  # 重庆
        '510000': '1935612311051014168',  # 四川
        '520000': '1935612311051014169',  # 贵州
        '530000': '1935612311051014170',  # 云南
        '540000': '1935612311051014171',  # 西藏
        '610000': '1935612311051014172',  # 陕西
        '620000': '1935612311051014173',  # 甘肃
        '630000': '1935612311051014174',  # 青海
        '640000': '1935612311051014175',  # 宁夏
        '650000': '1935612311051014176',  # 新疆
    }
    return mapping

def generate_sql(provinces, cities, counties):
    """生成完整的SQL脚本"""
    sql_lines = []
    dic_id_mapping = get_dic_id_mapping()
    
    # SQL文件头部
    sql_lines.append("-- 全国行政区代码数据插入脚本")
    sql_lines.append("-- 自动生成，包含全部省市县区数据")
    sql_lines.append("")
    sql_lines.append("BEGIN;")
    sql_lines.append("")
    
    # 生成省级数据
    sql_lines.append("-- 省级app_dic_define记录")
    province_codes = [
        ('BJXZQDM', '北京行政区代码', 1),
        ('TJXZQDM', '天津行政区代码', 2),
        ('HEBXZQDM', '河北行政区代码', 3),
        ('SXXZQDM', '山西行政区代码', 4),
        ('NMGXZQDM', '内蒙古行政区代码', 5),
        ('LNXZQDM', '辽宁行政区代码', 6),
        ('JLXZQDM', '吉林行政区代码', 7),
        ('HLJXZQDM', '黑龙江行政区代码', 8),
        ('SHXZQDM', '上海行政区代码', 9),
        ('JSXZQDM', '江苏行政区代码', 10),
        ('ZJXZQDM', '浙江行政区代码', 11),
        ('AHXZQDM', '安徽行政区代码', 12),
        ('FJXZQDM', '福建行政区代码', 13),
        ('JXXZQDM', '江西行政区代码', 14),
        ('SDXZQDM', '山东行政区代码', 15),
        ('HENXZQDM', '河南行政区代码', 16),
        ('HUBXZQDM', '湖北行政区代码', 17),
        ('HUNXZQDM', '湖南行政区代码', 18),
        ('GDXZQDM', '广东行政区代码', 19),
        ('GXXZQDM', '广西行政区代码', 20),
        ('HNXZQDM', '海南行政区代码', 21),
        ('CQXZQDM', '重庆行政区代码', 22),
        ('SCXZQDM', '四川行政区代码', 23),
        ('GZXZQDM', '贵州行政区代码', 24),
        ('YNXZQDM', '云南行政区代码', 25),
        ('XZXZQDM', '西藏行政区代码', 26),
        ('SHAANXZQDM', '陕西行政区代码', 27),
        ('GSXZQDM', '甘肃行政区代码', 28),
        ('QHXZQDM', '青海行政区代码', 29),
        ('NXXZQDM', '宁夏行政区代码', 30),
        ('XJXZQDM', '新疆行政区代码', 31),
    ]
    
    sql_lines.append("INSERT INTO public.app_dic_define (id, code, name, remark, sort, type_id) VALUES")
    define_values = []
    # 使用dic_id_mapping中的值作为app_dic_define的ID，确保与app_dic_entry的dic_id匹配
    province_order = ['110000', '120000', '130000', '140000', '150000', '210000', '220000', '230000', 
                     '310000', '320000', '330000', '340000', '350000', '360000', '370000', '410000',
                     '420000', '430000', '440000', '450000', '460000', '500000', '510000', '520000',
                     '530000', '540000', '610000', '620000', '630000', '640000', '650000']
    
    for i, (code, name, sort) in enumerate(province_codes):
        province_code = province_order[i]
        dic_id = dic_id_mapping[province_code]  # 使用映射中的dic_id
        define_values.append(f"('{dic_id}', '{code}', '{name}', '', {sort}, '1935612154926436353')")
    sql_lines.append(",\n".join(define_values) + ";")
    sql_lines.append("")
    
    # 直辖市代码
    direct_cities = ['110000', '120000', '310000', '500000']
    
    # 为每个省生成行政区数据
    for province_code in sorted(provinces.keys()):
        province_name = provinces[province_code]
        dic_id = dic_id_mapping.get(province_code, generate_uuid())
        
        sql_lines.append(f"-- {province_name}行政区代码")
        
        # 生成该省的所有记录
        all_entries = []
        
        # 1. 省级记录
        province_id = generate_uuid()
        all_entries.append(f"('{province_id}', '{province_code}', '{province_name}', null, '{dic_id}', '', 1)")
        
        # 2. 地市级记录
        city_sort = 2
        city_id_mapping = {}  # 存储城市代码到ID的映射
        
        if province_code in cities:
            for city_code, city_name in sorted(cities[province_code]):
                city_id = generate_uuid()
                city_id_mapping[city_code] = city_id
                if province_code in direct_cities:
                    # 直辖市：市级记录的父节点是省级
                    all_entries.append(f"('{city_id}', '{city_code}', '{city_name}', '{province_id}', '{dic_id}', '', {city_sort})")
                else:
                    # 其他省份：市级记录的父节点是省级
                    all_entries.append(f"('{city_id}', '{city_code}', '{city_name}', '{province_id}', '{dic_id}', '', {city_sort})")
                city_sort += 1
        
        # 3. 县区级记录 - 按市级分组
        if province_code in direct_cities:
            # 直辖市：区县直接属于省级
            county_sort = city_sort
            province_counties = [c for c in counties if c[3] == province_code]
            for county_code, county_name, _, _ in sorted(province_counties):
                all_entries.append(f"('{generate_uuid()}', '{county_code}', '{county_name}', '{province_id}', '{dic_id}', '', {county_sort})")
                county_sort += 1
        else:
            # 其他省份：按市级分组县区
            for city_code in sorted(city_id_mapping.keys()):
                city_id = city_id_mapping[city_code]
                county_sort = 1
                
                # 获取该市下的所有县区
                city_counties = [c for c in counties if c[2] == city_code and c[3] == province_code]
                for county_code, county_name, _, _ in sorted(city_counties):
                    all_entries.append(f"('{generate_uuid()}', '{county_code}', '{county_name}', '{city_id}', '{dic_id}', '', {county_sort})")
                    county_sort += 1
        
        # 输出该省的所有INSERT语句
        if all_entries:
            sql_lines.append("INSERT INTO public.app_dic_entry (id, code, name, pid, dic_id, remark, sort) VALUES")
            sql_lines.append(",\n".join(all_entries) + ";")
            sql_lines.append("")
    
    sql_lines.append("COMMIT;")
    
    return "\n".join(sql_lines)

def main():
    """主函数"""
    try:
        # 解析数据文件
        provinces, cities, counties = parse_xzqdm_file('data/xzqdm.txt')
        
        print(f"解析完成：")
        print(f"省份数量: {len(provinces)}")
        print(f"地市数量: {sum(len(city_list) for city_list in cities.values())}")
        print(f"县区数量: {len(counties)}")
        
        # 生成SQL
        sql_content = generate_sql(provinces, cities, counties)
        
        # 写入文件
        with open('xzqdm_complete.sql', 'w', encoding='utf-8') as f:
            f.write(sql_content)
        
        print("SQL文件生成完成: xzqdm_complete.sql")
        
    except Exception as e:
        print(f"错误: {e}")

if __name__ == "__main__":
    main() 