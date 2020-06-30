/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import java.util.List;
import java.util.Set;

import org.hibernate.sql.ast.tree.SqlAstTreeLogger;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 * Logs a debug representation of the SQL AST.
 *
 * NOTE : at the moment, we only render the from-elements
 *
 * @author Steve Ebersole
 */
public class SqlTreePrinter {
	public static void logSqlAst(Statement sqlAstStatement) {
		if ( ! SqlAstTreeLogger.DEBUG_ENABLED ) {
			return;
		}

		final SqlTreePrinter printer = new SqlTreePrinter();
		printer.visitStatement( sqlAstStatement );

		SqlAstTreeLogger.INSTANCE.debugf( "SQL AST Tree:%n" + printer.buffer.toString() );
	}

	private final StringBuffer buffer = new StringBuffer();
	private int depth = 2;

	private SqlTreePrinter() {
	}

	private void visitStatement(Statement sqlAstStatement) {
		if ( sqlAstStatement instanceof SelectStatement ) {
			final SelectStatement selectStatement = (SelectStatement) sqlAstStatement;
			logNode(
					"SelectStatement",
					() -> visitFromClause( selectStatement.getQuerySpec().getFromClause() )
			);
		}
		else if ( sqlAstStatement instanceof DeleteStatement ) {
			final DeleteStatement deleteStatement = (DeleteStatement) sqlAstStatement;
			logNode(
					"DeleteStatement",
					() -> logWithIndentation(
							"target : " + deleteStatement.getTargetTable().getTableExpression()
					)
			);
		}
		else if ( sqlAstStatement instanceof UpdateStatement ) {
			final UpdateStatement updateStatement = (UpdateStatement) sqlAstStatement;
			logNode(
					"UpdateStatement",
					() -> logWithIndentation(
							"target : " + updateStatement.getTargetTable().getTableExpression()
					)
			);
		}
		else if ( sqlAstStatement instanceof InsertStatement) {
			final InsertStatement insertStatement = (InsertStatement) sqlAstStatement;
			logNode(
					"InsertStatement",
					() -> logWithIndentation(
							"target : " + insertStatement.getTargetTable().getTableExpression()
					)
			);
		}
		else {
			throw new UnsupportedOperationException( "Printing for this type of SQL AST not supported : " + sqlAstStatement );
		}
	}

	private void visitFromClause(FromClause fromClause) {
		logNode(
				"FromClause",
				() -> fromClause.visitRoots( this::visitTableGroup )
		);
	}

	private void visitTableGroup(TableGroup tableGroup) {
		logNode(
				toDisplayText( tableGroup ),
				() -> logTableGroupDetails( tableGroup )
		);
	}

	private String toDisplayText(TableGroup tableGroup) {
		return tableGroup.getClass().getSimpleName() + " ("
					+ tableGroup.getGroupAlias() + " : "
					+ tableGroup.getNavigablePath()
					+ ")";
	}

	private void logTableGroupDetails(TableGroup tableGroup) {
		logWithIndentation(
				"navigablePath : %s,",
				tableGroup.getNavigablePath()
		);

		logWithIndentation(
				"modelPart : %s,",
				tableGroup.getModelPart().getNavigableRole()
		);

		logWithIndentation(
				"primaryTableReference : %s as %s,",
				tableGroup.getPrimaryTableReference().getTableExpression(),
				tableGroup.getPrimaryTableReference().getIdentificationVariable()
		);

		final List<TableReferenceJoin> tableReferenceJoins = tableGroup.getTableReferenceJoins();
		if ( ! tableReferenceJoins.isEmpty() ) {
			logNode(
					"tableReferenceJoins",
					() -> {
						for ( TableReferenceJoin join : tableReferenceJoins ) {
							logWithIndentation(
									"%s join %s as %s",
									join.getJoinType().getText(),
									join.getJoinedTableReference().getTableExpression(),
									join.getJoinedTableReference().getIdentificationVariable()
							);
						}
					}
			);
		}

		final Set<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		if ( ! tableGroupJoins.isEmpty() ) {
			logNode(
					"tableGroupJoins",
					() -> tableGroup.visitTableGroupJoins( this::visitTableGroupJoin )
			);
		}
	}

	private void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
		logNode(
				tableGroupJoin.getJoinType().getText() + " join " + toDisplayText( tableGroupJoin.getJoinedGroup() ),
				() -> logTableGroupDetails( tableGroupJoin.getJoinedGroup() )
		);
	}

	private void logNode(String text) {
		logWithIndentation( "%s", text );
	}

	private void logNode(String text, Runnable subTreeHandler) {
		logNode( text, subTreeHandler, false );
	}

	private void logNode(String text, Runnable subTreeHandler, boolean indentContinuation) {
		logWithIndentation( "%s {", text );
		depth++;

		try {
			if ( indentContinuation ) {
				depth++;
			}
			subTreeHandler.run();
		}
		catch (Exception e) {
			SqlAstTreeLogger.INSTANCE.debugf( e, "Error processing node {%s}", text );
		}
		finally {
			if ( indentContinuation ) {
				depth--;
			}
		}

		depth--;
		logWithIndentation( "}", text );
	}

	private void logWithIndentation(Object line) {
		pad( depth );
		buffer.append( line ).append( System.lineSeparator() );
	}

	private void logWithIndentation(String pattern, Object arg1) {
		logWithIndentation( String.format( pattern, arg1 ) );
	}

	private void logWithIndentation(String pattern, Object arg1, Object arg2) {
		logWithIndentation( String.format( pattern, arg1, arg2 ) );
	}

	private void logWithIndentation(String pattern, Object arg1, Object arg2, Object arg3) {
		logWithIndentation( String.format( pattern, arg1, arg2, arg3 ) );
	}

	private void pad(int depth) {
		for ( int i = 0; i < depth; i++ ) {
			buffer.append( "  " );
		}
	}

}
