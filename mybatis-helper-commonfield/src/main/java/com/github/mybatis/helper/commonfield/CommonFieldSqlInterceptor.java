package com.github.mybatis.helper.commonfield;

import com.github.mybatis.helper.commonfield.annotation.CommonFieldSettings;
import com.github.mybatis.helper.commonfield.dialect.DialectHandler;
import com.github.mybatis.helper.commonfield.dialect.helper.Dialect;
import com.github.mybatis.helper.core.sql.ExecuteHelper;
import com.github.mybatis.helper.core.sql.SqlInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;

/**
 * 查询或更新时自动写入创建人创建时间，更新人更新时间
 * @author will
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
@CommonFieldSettings
public class CommonFieldSqlInterceptor extends SqlInterceptor {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Dialect dialect;

    public CommonFieldSqlInterceptor(){
        super();
        paramName="userId";
    }
    @Override
    public String doSqlFilter(String originalSql,Object filterParam,MappedStatement mappedStatement,RowBounds rowBounds,BoundSql boundSql,ExecuteHelper executeHelper) {
        try{
            CommonFieldSettings activeSettings=getSetting(mappedStatement.getId());
            if(dialect==null){
                dialect = DialectHandler.getDialect(mappedStatement);
            }
            Expression today= dialect.buildNowExpression();
            if(mappedStatement.getSqlCommandType()==SqlCommandType.UPDATE){
                Update update = (Update) CCJSqlParserUtil.parse(originalSql);
                boolean isHasUp=false;
                boolean isHasUd=false;
                for(Column column:update.getColumns()){
                    if(!isHasUp&&column.getColumnName().equals(activeSettings.updatePersionColumn())){
                        isHasUp=true;
                        continue;
                    }
                    if(!isHasUd&&column.getColumnName().equals(activeSettings.updateDateColumn())){
                        isHasUd=true;
                    }
                }
                if(!isHasUp){
                    update.getColumns().add(new Column(activeSettings.updatePersionColumn()));
                    update.getExpressions().add(new LongValue(Long.parseLong(filterParam.toString())));
                }
                if(!isHasUd){
                    update.getColumns().add(new Column(activeSettings.updateDateColumn()));
                    update.getExpressions().add(today);
                }
                return update.toString();
            }else if (mappedStatement.getSqlCommandType()==SqlCommandType.INSERT){
                Insert insert = (Insert) CCJSqlParserUtil.parse(originalSql);
                List<Expression> insertExpressionList = ((ExpressionList) insert.getItemsList()).getExpressions();
                LongValue persion=new LongValue(Long.parseLong(filterParam.toString()));
                boolean isHasIp=false;
                boolean isHasId=false;
                boolean isHasUp=false;
                boolean isHasUd=false;
                for(Column column:insert.getColumns()){
                    if(!isHasUp&&column.getColumnName().equals(activeSettings.updatePersionColumn())){
                        isHasUp=true;
                        continue;
                    }
                    if(!isHasUd&&column.getColumnName().equals(activeSettings.updateDateColumn())){
                        isHasUd=true;
                        continue;
                    }
                    if(!isHasIp&&column.getColumnName().equals(activeSettings.insertPersionColumn())){
                        isHasIp=true;
                        continue;
                    }
                    if(!isHasId&&column.getColumnName().equals(activeSettings.insertDateColumn())){
                        isHasId=true;
                    }

                }
                if(!isHasIp){
                    insert.getColumns().add(new Column(activeSettings.insertPersionColumn()));
                    insertExpressionList.add(persion);
                }
                if(!isHasId){
                    insert.getColumns().add(new Column(activeSettings.insertDateColumn()));
                    insertExpressionList.add(today);
                }
                if(!isHasUp){
                    insert.getColumns().add(new Column(activeSettings.updatePersionColumn()));
                    insertExpressionList.add(new LongValue(Long.parseLong(filterParam.toString())));
                }
                if(!isHasUd){
                    insert.getColumns().add(new Column(activeSettings.updateDateColumn()));
                    insertExpressionList.add(today);
                }
                return insert.toString();
            }
        }catch (Exception e){
            logger.error("通用字段保存错误，sql：{}，原因：{}",originalSql,e);
        }
        return null;
    }

    @Override
    public boolean isContainsSqlCommandType(SqlCommandType sqlCommandType) {
        return sqlCommandType==SqlCommandType.INSERT||sqlCommandType==SqlCommandType.UPDATE;
    }
}
