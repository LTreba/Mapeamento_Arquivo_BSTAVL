//
// Exemplo de tokenizer (lexer) e parser.
// Copyright (C) 2024 André Kishimoto
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
//

/*	Apl2
 * 
 * Por: Joaquim Rafael Mariano Prieto Pereira  RA: 10408805 
 * Lucas Trebacchetti Eiras RA: 10401973
 * Henrique Árabe Neres de Farias RA: 10410152
 * Antonio Carlos Sciamarelli Neto  RA: 10409160
 * 
 * Estruturas de Dados II Professor Andre Kishimoto Sala 04G12
 * 
 * Consulta em: 
 * https://youtu.be/Gt2yBZAhsGM?si=WNOSZxaiCWmrA-sO
 * https://www.geeksforgeeks.org/binary-tree-data-structure/
 * https://www.ime.usp.br/~pf/mac0122-2003/aulas/bin-trees.html
 * https://www.javatpoint.com/binary-search-tree
 * PROGRAMIZ. AVL Tree. Disponível em: https://www.programiz.com/dsa/avl-tree.
 * GALLES, D. AVL Tree Visualization. Disponível em: https://www.cs.usfca.edu/~galles/visualization/AVLtree.html.
 * https://www.devmedia.com.br/java-arquivos-e-fluxos-de-dados/22859
 * 
 *
 *  e materias de sala:
 *  Árvore AVL (André Kishimoto)
 *  Revisão POO com Java (André Kishimoto)
 *  Herança em Java (André Kishimoto)
 *  Árvores - fundamentos (André Kishimoto)
 */

package parser;

import java.util.List;
import java.util.Stack;

/*
================================================================================
GRAMÁTICA
================================================================================
<data>         ::= ((<scope> | <key> | <comment>)* <blank_line>)*
<scope>        ::= <identifier> (<blank> | <blank_line>)* "(" <blank_line>+ <data>* <blank> ")"
<key>          ::= <identifier> <blank> "=" <blank> <value>
<identifier>   ::= <string>
<value>        ::= <string>
<comment>      ::= "#" <string>

<string>       ::= <char>+
<char>         ::= <basic_latin> | <latin_1_supp> | <whitespace>
<basic_latin>  ::= [\u0020-\u007F]  ; Unicode Basic Latin
<latin_1_supp> ::= [\u00A0-\u00FF]  ; Unicode Latin-1 Supplement

<blank_line>   ::= <blank> <newline>
<blank>        ::= <whitespace>*
<whitespace>   ::= " " | "\t"
<newline>      ::= "\n" | "\r" | "\r\n" 
*/

public class Parser {

	private List<Token> tokens;
	private Token currToken;
	private int index;
	Stack<String> stackParser = new Stack<String>();

	public Parser() {
		tokens = null;
		currToken = null;
		index = -1;
	}
	
	public void run(List<String> contents) {
		Tokenizer tokenizer = new Tokenizer();
		tokens = tokenizer.tokenize(contents);
		currToken = null;
		index = -1;

		// Descomente o código abaixo para ver a lista de tokens gerada pelo Tokenizer.
//		System.out.println("==================== TOKENS ====================");
		for (var token : tokens) {
			System.out.println(token);
		}
//		System.out.println("==================== TOKENS ====================");
		
		parse();
	}
	
	// O parser sempre começa avançando para o primeiro token da lista e, na sequência,
	// avalia a regra <code> (a regra mais geral da gramática).
	// Após processar <code>, o último token da lista deve ser o EOF, indicando que
	// todo o conteúdo foi processado corretamente.
	private void parse() {
		advance();
		data();		
		if (currToken.getType() != TokenType.EOF) {
			throw new RuntimeException("Parser.parse(): Esperado fim do conteúdo (EOF), mas encontrou " + currToken);
		}
		
		//Verificando abertura e fechamento do scope
		if(!stackParser.isEmpty()) {
			throw new RuntimeException("Parser.parse(): Correspondência de abertura e fechamento de escopos com erro!");
		}
	}
	
	// <code> ::= ((<print> | <sum>)* <blank_line>)*
	private void data() {
		TokenType type = currToken.getType();

		// Consome 0+ regras do tipo <print> e/ou <sum> seguida por <blank_line>.
		while (type == TokenType.STRING || type == TokenType.COMMENT || type == TokenType.NEWLINE) {
			if (type == TokenType.STRING) {
				verfifyScopeKey();
			} else if (type == TokenType.COMMENT) {
				comment();
			}
			
			// Após processar <comment> ou <scope> ou <key>, consome um <blank_line>.
			consume(TokenType.NEWLINE);
			
			// Neste exemplo, processamos a regra <blank_line> com uma quebra de linha na saída em tela.
			System.out.println();			
			
			type = currToken.getType();
		}
	}
	
	//Quando achamos uma String pura, teremos a possibilidade dela ser um scope ou uma key
	private void verfifyScopeKey() {
		//Variável usada para identificar se pelo menos existe um espaço antes do identificador
		// "="
		boolean keyWhiteSpace = false;
		consume(TokenType.STRING);
		//Consumindo espaços antes do identificador
		while (currToken.getType() == TokenType.WHITESPACE) {
			consume(TokenType.WHITESPACE);
			keyWhiteSpace = true;
		}
		//Verificando se é scope ou key
		if(currToken.getType() == TokenType.KEY && keyWhiteSpace) {
			key();
		}else if(currToken.getType() == TokenType.SCOPE) {
			scope();
		}else {
			//Quando dentro de um escopo encontramos uma string sem semântica
			throw new RuntimeException("Parser.parse(): Esperado fim do conteúdo (EOF), mas encontrou STRING");
		}
	}
	 // <scope> ::= <identifier> (<blank> | <blank_lines>)* "(" <blank_line>+ <data>* <blank> ")"
	private void scope() {
		//Consumir quebra de linhas e espaços em branco, caso exista
		while (currToken.getType() == TokenType.NEWLINE || currToken.getType() == TokenType.WHITESPACE) {
			if(currToken.getType() == TokenType.NEWLINE) {
				consume(TokenType.NEWLINE);
			}else {
				consume(TokenType.WHITESPACE);
			}
		}
		
		//Empilhando abertura para verificar correspondência
		if(currToken.getValue() == "(") {
			stackParser.push("(");
		}
		
		//Consumindo abertura "("
		consume(TokenType.SCOPE);
		
		//Consumindo uma ou mais newLines <blank_line>
		consume(TokenType.NEWLINE);
		while(currToken.getType() == TokenType.NEWLINE) {
			consume(TokenType.NEWLINE);
		}
		
		//Verificando agora a <data>
		while(currToken.getValue() != ")" || currToken.getType() != TokenType.EOF) {
			if (currToken.getType() == TokenType.COMMENT) {
				comment();
			}
			if (currToken.getType() == TokenType.STRING) {
				verfifyScopeKey();
			}
			if(currToken.getType() == TokenType.NEWLINE) {
				consume(TokenType.NEWLINE);
			}
			if(currToken.getType() == TokenType.WHITESPACE) {
				consume(TokenType.WHITESPACE);
			}
		}
		//Não chega nessa verificação
		
		//Desempilhando abertura ao achar fechamento
		if(currToken.getValue() == ")") {
			stackParser.pop();
		}
		//Consumindo fechamento
		consume(TokenType.SCOPE);
	}
	
	
	// <key> ::= <string> <blank> "=" <blank> <string>
	private void key() {
		//Consumindo o identificador '='
		consume(TokenType.KEY);
		
		//Consumindo os espaços em branco após o identificador '='
		while (currToken.getType() == TokenType.WHITESPACE) {
			consume(TokenType.WHITESPACE);
		}
		
		consume(TokenType.STRING);
	}
	
	// <comment> ::= "#" <string>
	private void comment() {
		// Consome "#".
		consume(TokenType.COMMENT);
		
		// Consome 0+ espaços em branco (<whitespace>*).
		while (currToken.getType() == TokenType.WHITESPACE) {
			consume(TokenType.WHITESPACE);
		}
		
		// Neste ponto, o esperado é ter um token STRING.
		
		// Consome <string>.
		consume(TokenType.STRING);
	}
	

	// Avança para o próximo token da lista (atualiza currToken).
	private void advance() {
		++index;
		if (index >= tokens.size()) {
			throw new RuntimeException("Fim de conteúdo inesperado.");
		}
		currToken = tokens.get(index);
	}
	
	// Consome um token esperado.
	// "Consumir um token" significa que o token atual é do tipo esperado por uma regra da gramática.
	// Caso seja o token esperado, avança para o próximo token da lista (atualiza currToken).
	// Caso contrário, lança uma exceção de token incorreto.
	private void consume(TokenType expected) {
		if (currToken.getType() == expected) {
			advance();
		} else {
			throw new RuntimeException("Parser.consume(): Token incorreto. Esperado: " + expected + ". Obtido: " + currToken);
		}
	}

}
